package com.pagecall;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Device;
import org.mediasoup.droid.MediasoupClient;
import org.mediasoup.droid.MediasoupException;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.RecvTransport;
import org.mediasoup.droid.SendTransport;
import org.mediasoup.droid.Transport;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class MediaInfraController extends MediaController {
    static class TransportPayload {
        // TODO @NonNull @Nullable Annotation
        String id;
        String iceParameters;
        String iceCandidates;
        String dtlsParameters;
        String sctpParameters;

        TransportPayload(JSONObject data) {
            this.id = data.optString("id");
            this.iceParameters = data.optString("iceParameters");
            this.iceCandidates = data.optString("iceCandidates");
            this.dtlsParameters = data.optString("dtlsParameters");
            this.sctpParameters = data.optString("sctpParameters");
        }

    }

    static class MiInitialPayload {
        String rtpCapabilities;
        TransportPayload send;
        TransportPayload recv;

        MiInitialPayload(JSONObject data) {
            this.rtpCapabilities = data.optString("rtpCapabilities");
            this.send = new TransportPayload(data.optJSONObject("send"));
            this.recv = new TransportPayload(data.optJSONObject("recv"));
        }
    }

    private Device device;
    private SendTransport sendTransport;
    private RecvTransport recvTransport;
    private Producer producer;
    private PeerConnectionFactory factory;
    private Consumer[] consumers;

    WebViewEmitter emitter;

    MediaInfraController(WebViewEmitter emitter, MiInitialPayload initialPayload, Context context) throws MediasoupException {
        MediasoupClient.initialize(context);

        this.emitter = emitter;
        this.device = new Device();
        this.device.load(initialPayload.rtpCapabilities, null);
        this.sendTransport = device.createSendTransport(
                sendTransportListener,
                initialPayload.send.id,
                initialPayload.send.iceParameters,
                initialPayload.send.iceCandidates,
                initialPayload.send.dtlsParameters,
                initialPayload.send.sctpParameters
        );
        this.recvTransport = device.createRecvTransport(
                recvTransportListener,
                initialPayload.recv.id,
                initialPayload.recv.iceParameters,
                initialPayload.recv.iceCandidates,
                initialPayload.recv.dtlsParameters,
                initialPayload.recv.sctpParameters
        );
        this.factory = this.createPeerConnectionFactory();
    }

    private PeerConnectionFactory createPeerConnectionFactory() {
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        return PeerConnectionFactory.builder()
                .setOptions(options)
                .createPeerConnectionFactory();
    }

    @Override
    public Boolean pauseAudio() {
        if (producer != null) {
            producer.pause();
            return true;
        }
        return false;
    }

    @Override
    public Boolean resumeAudio() {
        if (producer != null) {
            producer.resume();
            return true;
        }
        return false;
    }

    @Override
    public void start(final AudioProducerCallback callback) {
        MediaConstraints audioConstraints = new MediaConstraints();
        AudioSource audioSource = factory.createAudioSource(audioConstraints);
        AudioTrack audioTrack = factory.createAudioTrack("audio0", audioSource);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (producer != null) {
                    producer.close();
                }

                try {
                    producer = sendTransport.produce(
                            newProducer -> {
                                if (producer != null) {
                                    // TODO: mStore.removeProducer(producer.getId());
                                    producer = null;
                                }
                            },
                            audioTrack,
                            null,
                            null,
                            null
                    );

                    callback.onResult(null);
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onResult(e);
                }
            }
        });
    }

    interface AudioProducerCallback {
        void onResult(Exception error);
    }

    @Override
    public void dispose() {
        if (producer != null) {
            try {
                producer.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        if (sendTransport != null) {
            try {
                sendTransport.dispose(); // TODO close or dispose?
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
//        if (recvTransport != null) {
//            try {
//                recvTransport.dispose();
//            } catch(Exception e) {
//                e.printStackTrace();
//            }
//        }
    }

    private SendTransport.Listener sendTransportListener = new SendTransport.Listener() {
        @Override
        public String onProduce(
                Transport transport, String kind, String rtpParameters, String appData) {
            JSONObject payload = new JSONObject();
            try {
                payload.put("kind", kind);
                payload.put("rtpParameters", new JSONObject(rtpParameters));
                payload.put("appData", new JSONObject(appData));

                String producerId = emitter.requestSync(NativeBridgeRequest.PRODUCE, payload);
                return producerId;
            } catch (Exception e) {
                emitter.error("NativeError", e.toString());
                return "";
            }
        }

        @Override
        public String onProduceData(
                Transport transport, String sctpStreamParameters, String label, String protocol, String appData) {
            // TODO
            return "";
        }

        @Override
        public void onConnect(Transport transport, String dtlsParameters) {
            JSONObject parsedDtlsParameters;
            try {
                parsedDtlsParameters = new JSONObject(dtlsParameters);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("MiController", "Failed to parse dtlsParameters: " + dtlsParameters);
                return;
            }

            JSONObject json = new JSONObject();
            try {
                json.put("dtlsParameters", parsedDtlsParameters);

                if (transport.getId().equals(sendTransport.getId())) {
                    json.put("type", "send");
                    emitter.emit(NativeBridgeEvent.CONNECT_TRANSPORT, json);
                } else if (transport.getId().equals(recvTransport.getId())) {
                    json.put("type", "recv");
                    emitter.emit(NativeBridgeEvent.CONNECT_TRANSPORT, json);
                } else {
                    emitter.error("UnknownTransport", "Transport type is unknown (id: " + transport.getId() + ")");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("MiController", "Error creating JSON object.");
            }
        }

        @Override
        public void onConnectionStateChange(Transport transport, String connectionState) {
            emitter.log("ConnectionStateChange", connectionState);
        }

    };
    private RecvTransport.Listener recvTransportListener = new RecvTransport.Listener() {
        @Override
        public void onConnect(Transport transport, String dtlsParameters) {
            JSONObject parsedDtlsParameters;
            try {
                parsedDtlsParameters = new JSONObject(dtlsParameters);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("MiController", "Failed to parse dtlsParameters: " + dtlsParameters);
                return;
            }

            JSONObject json = new JSONObject();
            try {
                json.put("dtlsParameters", parsedDtlsParameters);

                if (transport.getId().equals(sendTransport.getId())) {
                    json.put("type", "send");
                    emitter.emit(NativeBridgeEvent.CONNECT_TRANSPORT, json);
                } else if (transport.getId().equals(recvTransport.getId())) {
                    json.put("type", "recv");
                    emitter.emit(NativeBridgeEvent.CONNECT_TRANSPORT, json);
                } else {
                    emitter.error("UnknownTransport", "Transport type is unknown (id: " + transport.getId() + ")");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("MiController", "Error creating JSON object.");
            }
        }

        @Override
        public void onConnectionStateChange(Transport transport, String connectionState) {
            emitter.log("ConnectionStateChange", connectionState);
        }
    };

    void consume(
            String sessionId,
            String id,
            String producerId,
            String kind,
            String rtpParameters,
            String type,
            String appData,
            boolean paused
    ) {
        try {
            Consumer consumer = recvTransport.consume(
                    newConsumer -> {
                        //  TODO: mConsumers.remove(c.getId());
                    },
                    id,
                    producerId,
                    kind,
                    rtpParameters,
                    appData
            );
            // emit done
        } catch (MediasoupException e) {
            e.printStackTrace();
            // handle error
        }
    }
}
