# Module cordova-plugin-dev

Cordovaプラグインの元ソースをAndroid Studioで認識・編集・コンパイルするnative専用モジュールです。
`kotlin-plugin/src/android`を直接参照し、Cordova APIはcompileOnlyで解決します。
CordovaアプリのAPKにはこのモジュールを組み込まず、plugin.xmlで導入したソースをコンパイルします。

# Package dev.example.kotlinplugin

WebからopenFeatureを受け付け、featureのMainActivityを起動する入口です。
