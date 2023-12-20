package one.d4d.sessionless.forms;

import burp.api.montoya.collaborator.CollaboratorPayloadGenerator;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import one.d4d.sessionless.hexcodearea.HexCodeAreaFactory;
import one.d4d.sessionless.presenter.PresenterStore;
import one.d4d.sessionless.rsta.RstaFactory;
import one.d4d.sessionless.utils.ErrorLoggingActionListenerFactory;

import java.net.URL;

import static burp.api.montoya.internal.ObjectFactoryLocator.FACTORY;

public class RequestEditorView extends EditorTab implements ExtensionProvidedHttpRequestEditor {
    private volatile HttpService httpService;

    public RequestEditorView(
            PresenterStore presenters,
            RstaFactory rstaFactory,
            Logging logging,
            UserInterface userInterface,
            CollaboratorPayloadGenerator collaboratorPayloadGenerator,
            boolean editable,
            boolean isProVersion) {
        super(
                presenters,
                rstaFactory,
                new HexCodeAreaFactory(logging, userInterface),
                collaboratorPayloadGenerator,
                new ErrorLoggingActionListenerFactory(logging),
                editable,
                isProVersion
        );
    }

    @Override
    public void setRequestResponse(HttpRequestResponse requestResponse) {
        HttpRequest httpRequest = requestResponse.request();
        URL targetURL;
        try {
            URL raw = new URL(httpRequest.url());
            targetURL = new URL(raw.getProtocol(),
                    raw.getAuthority(),
                    raw.getPath());
        } catch (Exception e) {
            targetURL = null;
        }
        httpService = httpRequest.httpService();
        presenter.setMessage(httpRequest.toByteArray().toString(), targetURL, null, httpRequest.parameters());
    }

    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        String content = requestResponse.request().toByteArray().toString();
        return presenter.isEnabled(content, null, requestResponse.request().parameters());
    }

    @Override
    public HttpRequest getRequest() {
        return FACTORY.httpRequest(httpService, presenter.getMessage());
    }
}