import { customAlphabet } from "nanoid";
const nanoid = customAlphabet("0123456789abcdefghijklmnopqrstuvwxyz", 10);

export type Callback = (payload?: any) => void;

class RequestController {
  callbacks: { [requestId: string]: { resolve: Callback; reject: Callback } } =
    {};

  request(resolve: Callback, reject: Callback): string {
    const requestId = nanoid();
    this.callbacks[requestId] = { resolve, reject };
    return requestId;
  }

  response(requestId: string, payload: any): void {
    const callback = this.callbacks[requestId];
    if (!callback) return;
    callback.resolve(payload);
    delete this.callbacks[requestId];
  }

  throw(requestId: string, message: string = ""): void {
    const callback = this.callbacks[requestId];
    if (!callback) return;

    callback.reject(Error(message));
    delete this.callbacks[requestId];
  }
}

export default RequestController;
