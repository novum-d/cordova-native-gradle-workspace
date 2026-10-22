/**
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/

// Cordovaの初期化完了後にプラグインを呼べるよう、画面の操作を有効化する。
document.addEventListener('deviceready', onDeviceReady, false);

/** プラグイン登録後に起動ボタンを有効にし、WebからKotlin画面への入口を用意する。 */
function onDeviceReady() {
    console.log('Running cordova-' + cordova.platformId + '@' + cordova.version);
    document.getElementById('deviceready').classList.add('ready');
    const button = document.getElementById('open-feature');
    const status = document.getElementById('feature-status');
    button.disabled = false;
    button.addEventListener('click', function () {
        button.disabled = true;
        status.textContent = 'Opening Kotlin screen...';
        // 成功はActivity起動要求の完了を表す。画面から戻ったときの結果ではない。
        window.KotlinPlugin.openFeature(function () {
            status.textContent = 'Kotlin screen opened.';
            button.disabled = false;
        }, function (error) {
            status.textContent = 'Could not open Kotlin screen: ' + error;
            button.disabled = false;
        });
    });
}
