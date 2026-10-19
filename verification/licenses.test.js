"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { checkRuntime, checkPreparedAssets, render } = require("../scripts/licenses.js");
const reviewed = { variants: ["debug", "release"], components: [
    { coordinate: "example:library:1.0", artifact: "library-1.0.jar", sha256: "reviewed" }
] };
const artifact = { coordinate: "example:library:1.0", path: "/cache/library-1.0.jar", sha256: "reviewed" };
const actual = () => ({ debug: [{ ...artifact }], release: [{ ...artifact }] });

test("accepts reviewed runtime artifacts for both variants", () => {
    assert.doesNotThrow(() => checkRuntime(actual(), reviewed));
});
test("rejects a new transitive dependency or version", () => {
    const inventory = actual();
    inventory.release.push({ ...artifact, coordinate: "example:transitive:2.0" });
    assert.throws(() => checkRuntime(inventory, reviewed), /License review required: release/);
});
test("rejects changed artifacts under an unchanged coordinate", () => {
    const inventory = actual();
    inventory.debug[0].sha256 = "changed";
    assert.throws(() => checkRuntime(inventory, reviewed), /License review required: debug/);
});
test("requires both variant inventories", () => {
    assert.throws(() => checkRuntime({ debug: [{ ...artifact }] }, reviewed), /Missing runtime inventory: release/);
});
test("rejects empty or stale inventories", () => {
    assert.throws(() => checkRuntime({ debug: [], release: [] }, reviewed), /Empty runtime inventory/);
    const stale = { ...reviewed, components: [...reviewed.components, { coordinate: "removed:library:1.0" }] };
    assert.throws(() => checkRuntime(actual(), stale), /inventory is stale/);
});
test("bundled text contains full notices and Apache and Boost terms", () => {
    const outputs = render();
    const text = outputs["CordovaComposeApp/www/licenses/THIRD_PARTY_NOTICES.txt"];
    assert.match(text, /Copyright 2012 The Apache Software Foundation/);
    assert.match(text, /Copyright 2015-2020 The Apache Software Foundation/);
    assert.match(text, /Copyright 2016-2021 JetBrains/);
    assert.match(text, /Boost Software License - Version 1.0/);
    assert.match(text, /END OF TERMS AND CONDITIONS/);
});

test("rejects stale prepared assets even when source notices are current", () => {
    const directory = fs.mkdtempSync(path.join(os.tmpdir(), "cordova-licenses-"));
    try {
        fs.copyFileSync(path.join(__dirname, "../CordovaComposeApp/www/index.html"), path.join(directory, "index.html"));
        fs.mkdirSync(path.join(directory, "licenses"));
        for (const [relative, contents] of Object.entries(render())) {
            const prefix = "CordovaComposeApp/www/";
            if (relative.startsWith(prefix)) fs.writeFileSync(path.join(directory, relative.slice(prefix.length)), contents);
        }
        assert.doesNotThrow(() => checkPreparedAssets(directory));
        fs.writeFileSync(path.join(directory, "licenses/THIRD_PARTY_NOTICES.txt"), "stale notices");
        assert.throws(() => checkPreparedAssets(directory), /Stale prepared license asset/);
    } finally { fs.rmSync(directory, { recursive: true, force: true }); }
});
