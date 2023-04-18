import type { MediaStat } from "@pagecall/common";
import ListenerController, { Listener } from "./ListenerController";
import RequestController, { Callback } from "./RequestController";

function registerGlobals() {
  const requestController = new RequestController();
  const listenerController = new ListenerController();

  const postMessage = (
    data: { action: string; payload?: string },
    callback?: { resolve: Callback; reject: Callback }
  ) => {
    const { action, payload } = data;
    const requestId = callback
      ? requestController.request(callback.resolve, callback.reject)
      : undefined;
    window.pagecallAndroidBridge.postMessage(
      JSON.stringify({ action, payload, requestId })
    );
  };

  const pagecallNativePrivate = {
    emit: (eventName: string, payload?: string, eventId?: string) => {
      const parsedPayload = payload ? JSON.parse(payload) : undefined;
      if (eventId) {
        const callback = (error: string | null, result?: string) => postMessage({
          action: 'response',
          payload: JSON.stringify({ eventId, error, result })
        });
        listenerController.emit(eventName, parsedPayload, callback);
      } else {
        listenerController.emit(eventName, parsedPayload)
      }
    },

    response: (requestId: string, payload?: string) => {
      const parsedPayload = payload ? JSON.parse(payload) : undefined;
      requestController.response(requestId, parsedPayload);
    },

    throw: (requestId: string, message?: string) => {
      requestController.throw(requestId, message);
    },
  };

  const pagecallNativePublicStatic = {
    getPlatform: () => 'ios',
    useNativeMediaStore: () => true,
    version: 1,
    addListener(eventName: string, listener: Listener) {
      listenerController.addListener(eventName, listener);
    },
    removeListener(eventName: string, listener: Listener) {
      listenerController.removeListener(eventName, listener);
    },
  };
  const pagecallNative = new Proxy({
    ...pagecallNativePrivate,
    ...pagecallNativePublicStatic
  }, {
    get(staticMethods, action) {
      const staticMethod = staticMethods[action as keyof typeof staticMethods];
      if (staticMethod) return staticMethod;
      if (typeof action === 'symbol') throw new Error('Invalid access');
      return (payload: unknown) => new Promise((resolve, reject) => {
        postMessage(
          { action, payload: JSON.stringify(payload) },
          { resolve, reject }
        );
      });
    }
  });

  window.PagecallNative = pagecallNative
}

registerGlobals();
