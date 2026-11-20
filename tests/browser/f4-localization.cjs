// Actual localized MVC fixture HTML. Farm name "Today" is deliberately user data.
const {chromium}=require('playwright'); const fs=require('fs'); const assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.ANIGO_CHROME || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'});
 const output=process.env.ANIGO_REVIEW_OUTPUT || '/private/tmp/anigo-f4-review'; fs.mkdirSync(output,{recursive:true});
 for(const width of [320,390,768,1024,1440]) for(const mode of ['light','dark']){
  const context=await browser.newContext({viewport:{width,height:900},reducedMotion:'reduce'});
  await context.addInitScript(mode=>localStorage.setItem('anigo-color-mode',mode),mode);
  const page=await context.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.route('**/Notifications/AniAlerts',r=>r.fulfill({json:{items:[]}}));
  for(const screen of ['dashboard','tasks-today','tasks-mine','tasks-upcoming']){
   await page.route('**/F4*',r=>r.fulfill({contentType:'text/html',body:fs.readFileSync('tests/browser/authenticated-fixtures/f4-'+screen+'-fil.html','utf8')}));
   await page.goto('http://127.0.0.1:5082/F4?'+screen);await page.evaluate(()=>document.fonts.ready);
   assert.equal(await page.locator('html').getAttribute('lang'),'fil');
   assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+2),screen+' '+width+' overflow');
   assert.deepEqual(errors,[]);
   if(screen==='dashboard'){
    assert.equal(await page.locator('.mvp-farm-context').textContent(),'Today');
    assert.equal(await page.locator('h1').textContent(),'Ngayon');
    assert.deepEqual(await page.locator('#mobile-workspace-links a span').allTextContents(),['Ngayon','Pananim at Kalendaryo','Mga suplay','Mga ani']);
   }
   await page.screenshot({path:output+'/'+screen+'-'+width+'-'+mode+'.png',fullPage:true});await page.unroute('**/F4*');
  }
  await context.close();
 }
 await browser.close();console.log('PASS: Filipino farm/tasks filters, stable user farm name, localized nav, 5 widths × 2 themes; no overflow/JS errors');
})().catch(e=>{console.error(e);process.exit(1)});
