/** CordovaのJSブリッジを通して、KotlinPluginへ画面の起動要求を渡す。 */
'use strict';

const exec = require('cordova/exec');

/** 成功・失敗のコールバックを受け取り、引数なしでfeature画面を起動する。 */
exports.openFeature = function (success, error) {
    exec(success, error, 'KotlinPlugin', 'openFeature', []);
};
