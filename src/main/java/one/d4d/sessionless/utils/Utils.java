package one.d4d.sessionless.utils;

import com.google.common.primitives.Ints;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.exbin.deltahex.swing.CodeArea;
import org.exbin.utils.binary_data.BinaryData;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Utils {
    public static final int WORDLIST_ONE_CHAR = 256;
    public static final int WORDLIST_TWO_CHAR = 65536;
    public static final int WORDLIST_THREE_CHAR = 16_777_216;
    private static final String RESOURCE_BUNDLE = "strings";
    private static final String BASE64_REGEX = "[A-Za-z0-9-_]";
    private static final Pattern HEX_PATTERN = Pattern.compile("^([0-9a-fA-F]{2})+$");
    private static final Pattern BASE64_PATTERN = Pattern.compile(String.format("^%s+$", BASE64_REGEX));


    public static String compressBase64(byte[] value) {
        Deflater compressor = new Deflater();
        compressor.setInput(value);
        compressor.finish();
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        byte[] readBuffer = new byte[1024];
        int readCount = 0;
        while (!compressor.finished()) {
            readCount = compressor.deflate(readBuffer);
            if (readCount > 0) {
                bao.write(readBuffer, 0, readCount);
            }
        }
        compressor.end();
        String encoded = new String(Base64.getUrlEncoder().withoutPadding().encode(bao.toByteArray()));
        return String.format(".%s", encoded);
    }

    public static byte[] base64Decompress(byte[] value) throws DataFormatException {
        if (value[0] == '.') {
            byte[] data = Arrays.copyOfRange(value, 1, value.length);
            data = Base64.getUrlDecoder().decode(data);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
            Inflater decompressor = new Inflater();
            try {
                decompressor.setInput(data);
                final byte[] buf = new byte[1024];
                while (!decompressor.finished()) {
                    int count = decompressor.inflate(buf);
                    bos.write(buf, 0, count);
                }
            } finally {
                decompressor.end();
            }
            return bos.toByteArray();
        } else {
            return Base64.getUrlDecoder().decode(value);
        }
    }

    public static byte[] normalization(byte[] signature) {
        try {
            return hexdigest2byte(new String(signature));
        } catch (NumberFormatException iae) {
            return Base64.getUrlDecoder().decode(signature);
        }
    }

    public static byte[][] split(byte[] data, byte sep) {
        ArrayList<Integer> offsets = new ArrayList<>();

        for (int i = 0; i < data.length; i++) {
            if (data[i] == sep) {
                offsets.add(i);
            }
        }

        offsets.add(data.length);

        byte[][] ret = new byte[offsets.size()][];

        int index = 0;
        for (int i = 0; i < offsets.size(); i++) {
            ret[i] = new byte[offsets.get(i) - index];
            System.arraycopy(data, index, ret[i], 0, ret[i].length);
            index = offsets.get(i) + 1;
        }

        return ret;
    }

    public static byte[] normalizationWithDecompression(byte[] message) throws DataFormatException {
        try {
            return hexdigest2byte(new String(message));
        } catch (NumberFormatException iae) {
            return base64Decompress(message);
        }
    }

    public static byte[] hexdigest2byte(String hexdigest) throws NumberFormatException {
        if (hexdigest.length() < 2) throw new NumberFormatException();
        byte[] ans = new byte[hexdigest.length() / 2];

        for (int i = 0; i < ans.length; i++) {
            int index = i * 2;
            int val = Integer.parseInt(hexdigest.substring(index, index + 2), 16);
            ans[i] = (byte) val;
        }
        return ans;
    }

    public static byte[] timestampInFuture() {
        long ts = Instant.now().plusSeconds(31536000).getEpochSecond();
        return Base64.getUrlEncoder().withoutPadding().encode(Ints.toByteArray((int) ts));
    }

    public static byte[] timestampSecondsInFuture() {
        long ts = Instant.now().plusSeconds(31536000).getEpochSecond();
        return String.valueOf(ts).getBytes();
    }

    public static String base64timestamp(byte[] ts) {
        return timestamp(Base64.getUrlDecoder().decode(ts));
    }

    public static String base62timestamp(byte[] ts) throws Exception {
        Base62 standardEncoder = Base62.createInstance();
        if (!standardEncoder.isBase62Encoding(ts)) throw new Exception("Not a timestamp!");
        return timestamp(standardEncoder.decode(ts));
    }

    public static String timestamp(byte[] ts) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
            return formatter.format(Instant.ofEpochSecond(Ints.fromByteArray(ts)));
        } catch (Exception e) {
            return "Not a timestamp";
        }
    }

    public static String encodeBase64TimestampFromDate(String input) {
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = parser.parse(input);
            long ts = date.toInstant().getEpochSecond();
            return new String(Base64.getUrlEncoder().withoutPadding().encode(Ints.toByteArray((int) ts)));
        } catch (Exception e) {
            return new String(timestampInFuture());
        }
    }

    public static String encodeBase62TimestampFromDate(String input) {
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = parser.parse(input);
            long ts = date.toInstant().getEpochSecond();
            Base62 standardEncoder = Base62.createInstance();
            return new String(standardEncoder.encode(Ints.toByteArray((int) ts)));
        } catch (Exception e) {
            return new String(timestampInFuture());
        }
    }

    public static String timestampFromDateInSeconds(String input) {
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = parser.parse(input);
            int ts = (int) date.toInstant().getEpochSecond();
            return String.valueOf(ts);
        } catch (Exception e) {
            return new String(timestampSecondsInFuture());
        }
    }

    public static String timestampSeconds(String input) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
            return formatter.format(Instant.ofEpochSecond(Integer.parseInt(input)));
        } catch (Exception e) {
            return "Not a timestamp";
        }
    }

    public static boolean isValidJSON(String json) {
        try {
            JsonParser.parseString(json);
        } catch (JsonSyntaxException e) {
            return false;
        }
        return true;
    }
    public static boolean isValidJSON(byte[] json) {
        try {
            JsonParser.parseString(new String(json));
        } catch (JsonSyntaxException e) {
            return false;
        }
        return true;
    }

    public static byte[] prettyPrintJSON(byte[] json) {
        return prettyPrintJSON(json, 4);
    }

    public static String prettyPrintJSON(String json) {
        return prettyPrintJSON(json, 4);
    }

    public static byte[] prettyPrintJSON(byte[] json, int indentation) {

        // Strip any whitespace from the JSON string, also ensures the string actually contains valid JSON

        StringBuilder stringBuilder = new StringBuilder();

        // Simple pretty printer that increases indentation for every new Object or Array and places each key/value pair on a new line
        int indentationLevel = 0;
        boolean stringContext = false;
        for (byte b : json) {
            char c = (char) b;

            if (stringContext) {
                stringBuilder.append(c);
            } else {
                if (c == '{' || c == '[') {
                    indentationLevel++;
                    stringBuilder.append(c);
                    stringBuilder.append('\n');
                    stringBuilder.append(StringUtils.repeat(' ', indentationLevel * indentation));
                } else if (c == '}' || c == ']') {
                    indentationLevel--;
                    stringBuilder.append('\n');
                    stringBuilder.append(StringUtils.repeat(' ', indentationLevel * indentation));
                    stringBuilder.append(c);
                } else if (c == ':') {
                    stringBuilder.append(": ");
                } else if (c == ',') {
                    stringBuilder.append(",\n");
                    stringBuilder.append(StringUtils.repeat(' ', indentationLevel * indentation));
                } else {
                    stringBuilder.append(c);
                }
            }

            if (c == '"') {
                stringContext = !stringContext;
            }
        }
        return stringBuilder.toString().getBytes();
    }

    public static String prettyPrintJSON(String json, int indentation) {

        StringBuilder stringBuilder = new StringBuilder();

        // Simple pretty printer that increases indentation for every new Object or Array and places each key/value pair on a new line
        int indentationLevel = 0;
        boolean stringContext = false;
        for (char c : json.toCharArray()) {

            if (stringContext) {
                stringBuilder.append(c);
            } else {
                if (c == '{' || c == '[') {
                    indentationLevel++;
                    stringBuilder.append(c);
                    stringBuilder.append('\n');
                    stringBuilder.append(StringUtils.repeat(' ', indentationLevel * indentation));
                } else if (c == '}' || c == ']') {
                    indentationLevel--;
                    stringBuilder.append('\n');
                    stringBuilder.append(StringUtils.repeat(' ', indentationLevel * indentation));
                    stringBuilder.append(c);
                } else if (c == ':') {
                    stringBuilder.append(": ");
                } else if (c == ',') {
                    stringBuilder.append(",\n");
                    stringBuilder.append(StringUtils.repeat(' ', indentationLevel * indentation));
                } else {
                    stringBuilder.append(c);
                }
            }

            if (c == '"') {
                stringContext = !stringContext;
            }
        }
        return stringBuilder.toString();
    }

    public static void copyToClipboard(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
    }

    public static boolean isHex(String string) {
        return HEX_PATTERN.matcher(string).matches();
    }

    public static boolean isBase64URL(String string) {
        return BASE64_PATTERN.matcher(string).matches();
    }

    public static String getResourceString(String id) {
        return ResourceBundle.getBundle(RESOURCE_BUNDLE).getString(id);
    }

    public static List<String> readResourceForClass(final String fileName, Class clazz) {
        List<String> result = new ArrayList<>();
        try (InputStream inputStream = clazz.getResourceAsStream(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            Gson gson = new Gson();
            reader.lines().forEach(x -> result.add(gson.fromJson(x, String.class)));
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return result;
    }

    public static String compactJSON(String json) {

        StringBuilder stringBuilder = new StringBuilder();
        // Whitespace in JSON is four characters that are not inside a matched pair of double quotes
        boolean stringContext = false;
        for (char c : json.toCharArray()) {
            if (!stringContext && (c == 0x20 || c == 0x0A || c == 0x0D || c == 0x09)) {
                continue;
            }

            stringBuilder.append(c);

            if (c == '"') {
                stringContext = !stringContext;
            }
        }

        return stringBuilder.toString();
    }

    public static byte[] getCodeAreaData(CodeArea codeArea) {
        BinaryData binaryData = codeArea.getData();
        int size = (int) binaryData.getDataSize();
        byte[] data = new byte[size];
        binaryData.copyToArray(0L, data, 0, size);
        return data;
    }

    public static List<String> deserializeFile(File f) {
        List<String> result = new ArrayList<>();
        Gson gson = new Gson();
        try (Stream<String> lines = Files.lines(f.toPath())) {
            lines.forEach(s -> {
                try {
                    result.add(gson.fromJson(s, String.class));
                } catch (JsonSyntaxException e) {
                    result.add(s);
                }
            });
        } catch (IOException ex) {
            return result;
        }
        return result.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<String> generateWordlist(long l) {
        List<String> list = new ArrayList<>();
        for (; l < WORDLIST_ONE_CHAR; l++) {
            byte[] secret_key = new byte[]{(byte) l};
            list.add(new String(secret_key));
        }
        for (; l < WORDLIST_TWO_CHAR; l++) {
            byte[] secret_key = new byte[]{
                    (byte) (l >>> 8),
                    (byte) l};
            list.add(new String(secret_key));
        }
        for (; l < WORDLIST_THREE_CHAR; l++) {
            byte[] secret_key = new byte[]{
                    (byte) (l >>> 16),
                    (byte) (l >>> 8),
                    (byte) l};
            list.add(new String(secret_key));
        }
        return list;
    }
}

