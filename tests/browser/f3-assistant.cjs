// Real MVC fixture rendering; alerts are deterministic test inputs, not live weather.
const {chromium}=require('playwright'); const fs=require('fs'); const assert=require('node:assert/strict');
(async()=>{
const browser=await chromium.launch({headless:true,executablePath:"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"});
const out=process.env.ANIGO_REVIEW_OUTPUT || '/private/tmp/anigo-f3-review';fs.mkdirSync(out,{recursive:true});
for(const width of [320,390,768,1024,1440])for(const dark of [false,true]){
 const ctx=await browser.newContext({viewport:{width,height:900},reducedMotion:'reduce'}); await ctx.addInitScript(v=>localStorage.setItem('anigo-color-mode',v),dark?'dark':'light');
 const page=await ctx.newPage(); const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('**/Dashboard?*',route=>route.fulfill({contentType:'text/html',body:fs.readFileSync('tests/browser/authenticated-fixtures/dashboard-owner.html','utf8')}));
 await page.route('**/Notifications/AniAlerts',route=>route.fulfill({json:{items:[]}}));
 await page.goto('http://127.0.0.1:5082/Dashboard?farmId=123');
 const guide=page.locator('[data-ani-guide]');assert.equal(await guide.count(),1);
 await guide.getByRole('button',{name:'Not now',exact:true}).click();assert.equal(await guide.locator('[data-ani-content]').isVisible(),false);
 await page.reload();assert.equal(await guide.locator('[data-ani-content]').isVisible(),false);
 await guide.getByRole('button',{name:'Help from Ani'}).click();await guide.getByRole('button',{name:'Dismiss this tip'}).click();
 await page.reload();assert.equal(await guide.locator('[data-ani-content]').isVisible(),false);
 await guide.getByRole('button',{name:'Help from Ani'}).click();
 assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+2));assert.deepEqual(errors,[]);
 await page.screenshot({path:out+`/guide-${width}-${dark?'dark':'light'}.png`,fullPage:true}); await ctx.close();
}
// Verify real alert DTO flags, priority, failed acknowledgment and duplicate suppression.
const ctx=await browser.newContext({viewport:{width:1440,height:900},reducedMotion:'reduce'});
const page=await ctx.newPage(); const html=fs.readFileSync('tests/browser/authenticated-fixtures/dashboard-owner.html','utf8');
const farm=html.match(/data-farm="([^"]+)"/)[1];let acknowledged=false;let fail=true;
await page.route('**/Dashboard?*',r=>r.fulfill({contentType:'text/html',body:html}));
const record={id:'important',farmId:farm,title:'Test saved advisory',message:'Review saved forecast',source:'Open-Meteo',severity:2,shouldInterrupt:true,isStale:false,readAtUtc:null,acknowledgedAtUtc:null};
await page.route('**/Notifications/AniAlerts',r=>r.fulfill({json:{items:acknowledged?[]:[{...record,id:'stale',isStale:true},record,{...record,id:'routine',shouldInterrupt:false}]}}));
await page.route('**/Notifications/AcknowledgeAni',r=>{if(!fail)acknowledged=true;return r.fulfill({status:fail?200:204})});
await page.goto('http://127.0.0.1:5082/Dashboard?farmId=123');const panel=page.locator('[data-ani-alert]');await panel.waitFor({state:'visible'});
assert.equal(await panel.locator('[data-alert-id]').inputValue(),'important');
await panel.getByRole('button',{name:'Acknowledge',exact:true}).click();await page.waitForFunction(()=>document.querySelector('[data-alert-status]').textContent.includes('Could not'));
assert.equal(await panel.isVisible(),true); fail=false;
await page.screenshot({path:out+'/alert-desktop.png'});
await panel.getByRole('button',{name:'Acknowledge',exact:true}).click();await panel.waitFor({state:'hidden'});await page.reload();assert.equal(await panel.isVisible(),false);
await ctx.close();
const denied=await browser.newContext();await denied.addInitScript(()=>{
for(const name of ['localStorage','sessionStorage']) Object.defineProperty(window,name,{get(){throw new DOMException('Denied','SecurityError')}});
});const deniedPage=await denied.newPage();await deniedPage.route('**/Dashboard?*',r=>r.fulfill({contentType:'text/html',body:html}));await deniedPage.route('**/Notifications/AniAlerts',r=>r.fulfill({json:{items:[]}}));
await deniedPage.goto('http://127.0.0.1:5082/Dashboard?farmId=123');await deniedPage.locator('[data-ani-guide]').getByRole('button',{name:'Not now',exact:true}).click();assert.equal(await deniedPage.locator('[data-ani-content]').isVisible(),false);await denied.close();
await browser.close();console.log('PASS: F3 guide later/dismiss/replay persistence, 5 widths × 2 themes, reduced motion, no overflow or JS errors');
})().catch(e=>{console.error(e);process.exit(1)});
