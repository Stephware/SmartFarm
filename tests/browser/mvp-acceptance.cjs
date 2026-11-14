// Read-only live acceptance. Supply private test accounts/resources; never commit them.
// NODE_PATH must resolve the installed Playwright and axe-core packages.
const { chromium, firefox, webkit } = require('playwright');
const Axe = require('@axe-core/playwright').default;
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');

async function main() {
    const config = JSON.parse(fs.readFileSync(process.env.SMARTFARM_ACCEPTANCE_CONFIG, 'utf8'));
    const resources = JSON.parse(fs.readFileSync(process.env.SMARTFARM_ACCEPTANCE_RESOURCES, 'utf8'));
    const base = process.env.SMARTFARM_ACCEPTANCE_WEB;
    assert.ok(['localhost', '127.0.0.1'].includes(new URL(base).hostname), 'Local disposable host required');
    assert.ok(config.Database.startsWith('SmartFarmEnrollmentTest_'), 'Disposable database required');
    assert.match(resources.farmId, /^[\da-f-]{36}$/i);
    assert.match(resources.cycleId, /^[\da-f-]{36}$/i);
    const out = process.env.SMARTFARM_ACCEPTANCE_OUTPUT;
    fs.mkdirSync(out, { recursive: true, mode: 0o700 });
    const resultFile = path.join(out, 'results.json');
    assert.ok(!fs.existsSync(resultFile), 'Use a fresh output directory');
    const rows = [];
    const pages = [
        ['today', `/Dashboard?farmId=${resources.farmId}`],
        ['crops', `/Crops?farmId=${resources.farmId}`],
        ['supplies', `/Inventory?farmId=${resources.farmId}`],
        ['harvests', `/Reports/Harvests?farmId=${resources.farmId}`],
        ['water', `/Monitoring?farmId=${resources.farmId}&cycleId=${resources.cycleId}&quick=watering#record-activity`],
        ['inspect', `/Monitoring?farmId=${resources.farmId}&cycleId=${resources.cycleId}&quick=inspection#record-inspection`]
    ];
    for (const [engine, type] of [['chrome', chromium], ['firefox', firefox], ['webkit', webkit]]) {
        const browser = await type.launch(engine === 'chrome' ? { channel: 'chrome' } : {});
        try {
            for (const role of ['Owner', 'Manager', 'Worker']) {
                const context = await browser.newContext({ reducedMotion: 'reduce' });
                const page = await context.newPage();
                try {
                    await page.goto(base + '/Account/Login');
                    await page.locator('#Email').fill(config[role]);
                    await page.locator('#Password').fill(config.Password);
                    await Promise.all([
                        page.waitForURL(url => !url.pathname.includes('/Account/Login')),
                        page.locator('button[type=submit]').click()
                    ]);
                    for (const mode of ['light', 'dark']) {
                        await page.evaluate(value => localStorage.setItem('anigo-color-mode', value), mode);
                        for (const width of [390, 768, 1440]) {
                            await page.setViewportSize({ width, height: 900 });
                            for (const [name, url] of pages) {
                                const errors = [];
                                const onError = e => errors.push(e.message);
                                page.on('pageerror', onError);
                                const response = await page.goto(base + url, { waitUntil: 'networkidle' });
                                await page.evaluate(() => document.fonts.ready);
                                const facts = await page.evaluate(() => ({
                                    overflow: document.documentElement.scrollWidth > innerWidth,
                                    mode: document.documentElement.dataset.colorMode,
                                    primary: [...document.querySelectorAll('#mobile-workspace-links a span')].map(e => e.textContent.trim()),
                                    managerForm: !!document.querySelector('#planting-plans-heading'),
                                    dialogOverflow: [...document.querySelectorAll('dialog[open]')].some(d => d.scrollWidth > d.clientWidth + 1)
                                }));
                                const violations = width === 390 && mode === 'light'
                                    ? (await new Axe({ page }).analyze()).violations.map(v => ({ id: v.id, targets: v.nodes.map(n => n.target) })) : [];
                                const row = { engine, role, mode, width, name, status: response.status(), ...facts, errors, violations };
                                rows.push(row);
                                fs.writeFileSync(resultFile, JSON.stringify(rows, null, 2), { mode: 0o600 });
                                assert.equal(row.status, 200, `${engine}/${role}/${name} status`);
                                assert.equal(row.overflow || row.dialogOverflow, false, `${engine}/${role}/${name}/${width} overflow`);
                                assert.equal(row.mode, mode);
                                assert.deepEqual(row.errors, []);
                                assert.deepEqual(row.violations, [], `${engine}/${role}/${name} accessibility`);
                                assert.deepEqual(row.primary, ['Today', 'Crops & Calendar', 'Supplies', 'Harvests']);
                                if (role === 'Worker') assert.equal(row.managerForm, false);
                                if (role === 'Worker' && width === 390)
                                    await page.screenshot({ path: path.join(out, `${engine}-${name}-${mode}.png`), fullPage: true });
                                page.off('pageerror', onError);
                            }
                        }
                        await page.setViewportSize({ width: 390, height: 900 });
                        await page.goto(base + pages[0][1], { waitUntil: 'networkidle' });
                        await page.addStyleTag({ content: 'html { font-size: 200% !important; }' });
                        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false,
                            `${engine}/${role}/${mode} 200% text overflow`);
                        await page.screenshot({ path: path.join(out, `${engine}-${role}-${mode}-large-text.png`), fullPage: true });
                    }
                } finally { await context.close(); }
                console.log(`${engine}: ${role} responsive/theme/accessibility checks passed`);
            }
        } finally { await browser.close(); }
    }
    console.log(`${rows.length} live page variants passed; 200% text checked for each engine, role and theme.`);
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
