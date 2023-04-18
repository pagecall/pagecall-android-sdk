type PagecallNativeBridge = import("./PagecallNative").PagecallNativeBridge;

interface Window {
  PagecallNative: Partial<PagecallNativeBridge>;
  pagecallAndroidBridge: {
    postMessage: (message: any) => void;
  };
}
