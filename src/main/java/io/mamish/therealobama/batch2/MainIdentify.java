package io.mamish.therealobama.batch2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mamish.therealobama.batch.VoskRecogniserJson;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MainIdentify {

    public static void main(String[] args) throws IOException {

        var audioFileName = args[0];
        var voskModelDir = args[1];
        var outputFileName = args[2];

        Model voskModel = new Model(voskModelDir);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        VoskRecogniserJson voskJson;

        try (
                AudioInputStream audio = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream(audioFileName)));
                Recognizer recognizer = new Recognizer(voskModel, 16000)
        ) {
            int nbytes;
            byte[] b = new byte[4096];
            recognizer.setWords(true);

            while ((nbytes = audio.read(b)) >= 0) {
                recognizer.acceptWaveForm(b, nbytes);
            }

            voskJson = gson.fromJson(recognizer.getFinalResult(), VoskRecogniserJson.class);

        } catch (IOException | UnsupportedAudioFileException e) {
            throw new RuntimeException(e);
        }

        try (FileOutputStream out = new FileOutputStream(outputFileName)) {
            out.write(gson.toJson(voskJson).getBytes(StandardCharsets.UTF_8));
        }
    }
}
