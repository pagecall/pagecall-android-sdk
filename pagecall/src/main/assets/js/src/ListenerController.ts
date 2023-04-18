export type Listener = (payload?: string) => Promise<string | void>;

class ListenerController {
  listenersByEvent: {
    [eventName: string]: Set<Listener>
  } = {};

  addListener(
    eventName: string,
    listener: Listener
  ) {
    if (!this.listenersByEvent[eventName])
      this.listenersByEvent[eventName] = new Set<Listener>();
    this.listenersByEvent[eventName]?.add(listener);
  }

  removeListener(
    eventName: string,
    listener: Listener
  ) {
    const listeners = this.listenersByEvent[eventName];
    if (!listeners) return;
    listeners.delete(listener);
  }

  emit(eventName: string, payload: string, callback?: (error: string | null, result?: string) => void) {
    const listeners = this.listenersByEvent[eventName];
    if (!listeners) return;
    listeners.forEach(async (listener) => {
      try {
        const result = await listener(payload);
        callback?.(null, result ?? undefined);
      } catch (error) {
        callback?.((error as Error).message)
      }
    });
  }
}

export default ListenerController;
