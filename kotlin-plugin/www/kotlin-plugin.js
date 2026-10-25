/** CordovaのJSブリッジを通して、KotlinPluginへ画面の起動要求を渡す。 */
'use strict';

const exec = require('cordova/exec');

/**
 * 処理IDと任意のJSONオブジェクトをKotlinへ渡し、指定した画面を起動する。
 * @param {{id: string, args?: Object}} request 起動要求。業務IDはgolf-change.read-holder。home・sample.webviewは検証用。
 * @param {Function} success Activity起動要求が完了したときの通知。
 * @param {Function} error 引数不正・未対応ID・起動失敗の通知。
 */
exports.openFeature = function (request, success, error) {
    exec(success, error, 'KotlinPlugin', 'openFeature', [request]);
};
