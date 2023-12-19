package burp.config;

import burp.api.montoya.persistence.Preferences;
import com.google.gson.Gson;
import one.d4d.sessionless.utils.GsonHelper;
import one.d4d.sessionless.utils.Utils;

import java.io.File;
import java.util.List;

public class BurpKeysModelPersistence {
    static final String BURP_SETTINGS_NAME = "one.d4d.sessionless.keys";
    private final Preferences preferences;

    public BurpKeysModelPersistence(Preferences preferences) {
        this.preferences = preferences;
    }

    public KeysModel loadOrCreateNew() {
        String json = preferences.getString(BURP_SETTINGS_NAME);

        if (json == null) {
            KeysModel model = new KeysModel();
            model.setSalts(loadDefaultSalts());
            model.setSecrets(loadDefaultSecrets());
            return model;
        }

        Gson gson = GsonHelper.customGson;
        KeysModel keysModel = gson.fromJson(json, KeysModel.class);
        if (keysModel.getSaltsFilePath() != null) {
            List<String> result = Utils.deserializeFile(new File(keysModel.getSaltsFilePath()));
            if (result.isEmpty()) {
                keysModel.setSalts(loadDefaultSalts());
            } else {
                keysModel.setSalts(result);
            }
        }
        if (keysModel.getSecretsFilePath() != null) {
            List<String> result = Utils.deserializeFile(new File(keysModel.getSecretsFilePath()));
            if (result.isEmpty()) {
                keysModel.setSecrets(loadDefaultSecrets());
            } else {
                keysModel.setSecrets(result);
            }
        }
        return keysModel;
    }

    public void save(KeysModel model) {
        Gson gson = GsonHelper.customGson;
        String keysModeJson = gson.toJson(model);

        preferences.setString(BURP_SETTINGS_NAME, keysModeJson);
    }


    private List<String> loadDefaultSecrets() {
        return Utils.readResourceForClass("/secrets", this.getClass());
    }

    private List<String> loadDefaultSalts() {
        return Utils.readResourceForClass("/salts", this.getClass());
    }

}
