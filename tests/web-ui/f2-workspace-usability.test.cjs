"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const web = path.resolve(__dirname, "../../src/SmartFarm.Web");
const android = path.resolve(__dirname,
    "../../mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile");
const read = file => fs.readFileSync(path.join(web, file), "utf8");
const readAndroid = file => fs.readFileSync(path.join(android, file), "utf8");

test("Today card scrolls independently without allowing scroll-chaining", () => {
    const dashboard = read("Views/Dashboard/Index.cshtml");
    const styles = read("Views/Shared/_AniGoF2Styles.cshtml");
    assert.match(dashboard, /sf-home-task-list ani-f2-today-scroll/);
    assert.match(dashboard, /tabindex="0" aria-label="Today's work/);
    assert.match(styles, /overscroll-behavior-y: contain/);
    assert.match(styles, /overflow-y: auto/);
    assert.match(styles, /max-height: min\(390px, 48dvh\)/);
    assert.match(styles, /prefers-reduced-motion/);
});

test("Web calendar selects an individual day and hides until reopened", () => {
    const tasks = read("Views/Tasks/Index.cshtml");
    assert.match(tasks, /DateOnly\? selectedDay/);
    assert.match(tasks, /var calendarOpen = selectedDay is null/);
    assert.match(tasks, /@if \(calendarOpen && calendarMonth is/);
    assert.match(tasks, /asp-route-date="@date\.ToString\("yyyy-MM-dd"\)"/);
    assert.match(tasks, /asp-fragment="task-list-heading"/);
    assert.match(tasks, /asp-route-calendar="open"/);
    assert.match(tasks, /Tasks for @selectedDate/);
    assert.match(tasks, /Change date/);
    assert.match(tasks, /asp-route-month/);
    assert.match(tasks, /aria-label="@date\.ToString\("MMMM d"\), @due.Count tasks"/);
});

test("Long web records paginate without losing their filters or original export", () => {
    const tasks = read("Views/Tasks/Index.cshtml");
    const farms = read("Views/Farms/Index.cshtml");
    const crops = read("Views/Crops/Index.cshtml");
    const report = read("Views/Reports/Harvests.cshtml");
    const notifications = read("Views/Notifications/Index.cshtml");
    assert.match(tasks, /const int pageSize = 12/);
    assert.match(tasks, /visibleTasks/);
    assert.match(tasks, /Task result pages/);
    assert.match(farms, /visibleFarms/);
    assert.match(farms, /Farm pages/);
    assert.match(farms, /Join request pages/);
    assert.match(crops, /matchingCycleIndex/);
    assert.match(crops, /Crop record pages/);
    assert.match(report, /visibleEntries/);
    assert.match(report, /Harvest record pages/);
    assert.match(report, /asp-route-plotId="@report.PlotId"/);
    assert.match(report, /asp-route-cropTypeId="@report.CropTypeId"/);
    assert.match(report, /Export records CSV/);
    assert.match(notifications, /asp-fragment="notifications-list-heading" rel="next"/);
});

test("Android task calendar closes after date selection and large lists show more in batches", () => {
    const tasks = readAndroid("features/tasks/TaskScreens.kt");
    const farm = readAndroid("features/farms/FarmScreens.kt");
    const crops = readAndroid("features/crops/CropScreens.kt");
    const inventory = readAndroid("features/inventory/InventoryScreens.kt");
    assert.match(tasks, /rememberDatePickerState\(/);
    assert.match(tasks, /LaunchedEffect\(picker\.selectedDateMillis\)/);
    assert.match(tasks, /selectedView = "day"/);
    assert.match(tasks, /if \(selectedView == "day"\)/);
    assert.match(tasks, /"Change date"/);
    assert.match(tasks, /initialDisplayedMonthMillis = \(calendarDate \?: today\)/);
    assert.match(tasks, /Show 20 more completed tasks/);
    assert.match(tasks, /Show 20 more farm tasks/);
    assert.match(farm, /Show 20 more farms/);
    assert.match(farm, /Show 20 more requests/);
    assert.match(crops, /Show 20 more crop records/);
    assert.match(inventory, /Show 20 more supplies/);
});

test("Public landing removes unnecessary section buttons and developer-photo captions", () => {
    const landing = read("Views/Home/Index.cshtml");
    const layout = read("Views/Shared/_Layout.cshtml");
    const login = read("Views/Account/Login.cshtml");
    const register = read("Views/Account/Register.cshtml");
    for (const removed of ["#how-it-works", "#about", "#contact", "#features"]) {
        assert.ok(!layout.includes('href="' + removed + '"'), removed);
    }
    assert.doesNotMatch(landing, /Photo credits and sources|See how it works/);
    assert.doesNotMatch(login + register, /Agricultural photography for illustration only/);
    assert.match(landing, /How would you like to get started\?/);
    assert.match(landing, /I manage a farm|I work on a farm/);
    assert.match(layout, /aria-label="Account actions"/);
    assert.match(read("Views/Shared/_AniGoF2Styles.cshtml"), /ani-f2-pager/);
});
