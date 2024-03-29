package burp.proxy;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.proxy.websocket.*;
import burp.api.montoya.utilities.ByteUtils;
import burp.config.ProxyConfig;
import burp.config.SignerConfig;

public class ProxyWsMessageHandler implements ProxyMessageHandler {
    private final AnnotationsModifier annotationsModifier;

    public ProxyWsMessageHandler(ProxyConfig proxyConfig, SignerConfig signerConfig, ByteUtils byteUtils) {
        this.annotationsModifier = new AnnotationsModifier(proxyConfig, signerConfig, byteUtils);
    }

    @Override
    public TextMessageReceivedAction handleTextMessageReceived(InterceptedTextMessage interceptedTextMessage) {
        annotationsModifier.updateAnnotationsIfApplicable(
                interceptedTextMessage.annotations(),
                ByteArray.byteArray(interceptedTextMessage.payload()),
                null,
                null);

        return TextMessageReceivedAction.continueWith(interceptedTextMessage);
    }

    @Override
    public TextMessageToBeSentAction handleTextMessageToBeSent(InterceptedTextMessage interceptedTextMessage) {
        return TextMessageToBeSentAction.continueWith(interceptedTextMessage);
    }

    @Override
    public BinaryMessageReceivedAction handleBinaryMessageReceived(InterceptedBinaryMessage interceptedBinaryMessage) {
        annotationsModifier.updateAnnotationsIfApplicable(
                interceptedBinaryMessage.annotations(),
                interceptedBinaryMessage.payload(),
                null,
                null);

        return BinaryMessageReceivedAction.continueWith(interceptedBinaryMessage);
    }

    @Override
    public BinaryMessageToBeSentAction handleBinaryMessageToBeSent(InterceptedBinaryMessage interceptedBinaryMessage) {
        return BinaryMessageToBeSentAction.continueWith(interceptedBinaryMessage);
    }
}

