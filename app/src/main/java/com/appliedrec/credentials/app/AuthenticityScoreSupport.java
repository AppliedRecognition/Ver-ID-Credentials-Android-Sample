package com.appliedrec.credentials.app;

import android.content.Context;

import com.appliedrec.verid.core2.Classifier;
import com.microblink.entities.recognizers.blinkid.generic.BlinkIdCombinedRecognizer;
import com.microblink.entities.recognizers.blinkid.generic.classinfo.Region;
import com.microblink.entities.recognizers.blinkid.generic.classinfo.Type;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AuthenticityScoreSupport {

    public static synchronized AuthenticityScoreSupport defaultInstance() {
        HashMap<Region, Type[]> supportedDocuments = new HashMap<>();
        supportedDocuments.put(Region.ALBERTA, new Type[]{Type.DL, Type.ID});
        supportedDocuments.put(Region.BRITISH_COLUMBIA, new Type[]{Type.DL, Type.DL_PUBLIC_SERVICES_CARD, Type.ID, Type.PUBLIC_SERVICES_CARD});
        supportedDocuments.put(Region.MANITOBA, new Type[]{Type.DL, Type.ID});
        supportedDocuments.put(Region.NEW_BRUNSWICK, new Type[]{Type.DL});
        supportedDocuments.put(Region.NEWFOUNDLAND_AND_LABRADOR, new Type[]{Type.DL});
        supportedDocuments.put(Region.NOVA_SCOTIA, new Type[]{Type.DL});
        supportedDocuments.put(Region.ONTARIO, new Type[]{Type.DL, Type.ID});
        supportedDocuments.put(Region.QUEBEC, new Type[]{Type.DL});
        supportedDocuments.put(Region.SASKATCHEWAN, new Type[]{Type.DL});
        supportedDocuments.put(Region.YUKON, new Type[]{Type.DL});
        return new AuthenticityScoreSupport(supportedDocuments);
    }

    private HashMap<Region, Type[]> supportedDocuments;
    private Classifier[] classifiers = null;

    public AuthenticityScoreSupport(HashMap<Region, Type[]> supportedDocuments) {
        this.supportedDocuments = supportedDocuments;
    }

    public boolean isDocumentSupported(BlinkIdCombinedRecognizer.Result result) {
        for (Map.Entry<Region, Type[]> entry : this.supportedDocuments.entrySet()) {
            if (result.getClassInfo().getRegion() == entry.getKey()) {
                for (Type type : entry.getValue()) {
                    if (type == result.getClassInfo().getType()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Classifier[] getClassifiers(Context context) throws IOException {
        if (this.classifiers == null) {
            String[] assets = context.getAssets().list("");
            ArrayList<Classifier> classifierList = new ArrayList<>();
            for (String asset : assets) {
                if (asset.startsWith("licence") || asset.startsWith("license")) {
                    File licenceModelFile = new File(context.getFilesDir(), asset);
                    if (!licenceModelFile.exists()) {
                        try (InputStream inputStream = context.getAssets().open(asset)) {
                            try (FileOutputStream outputStream = new FileOutputStream(licenceModelFile)) {
                                int read;
                                byte[] buffer = new byte[256];
                                while ((read = inputStream.read(buffer, 0, buffer.length)) > 0) {
                                    outputStream.write(buffer, 0, read);
                                }
                                outputStream.flush();
                            }
                        }
                    }
                    classifierList.add(new Classifier("licence", licenceModelFile.getPath()));
                }
            }
            this.classifiers = new Classifier[classifierList.size()];
            classifierList.toArray(this.classifiers);
        }
        return this.classifiers;
    }

    public Classifier createLicenceAuthenticityClassifier(Context context) throws IOException {
        File[] licenceModelFiles = context.getFilesDir().listFiles(file -> file.getName().startsWith("licence") || file.getName().startsWith("license"));
        File licenceModelFile = null;
        if (licenceModelFiles != null && licenceModelFiles.length > 0) {
            Arrays.sort(licenceModelFiles, (a, b) -> new Date(b.lastModified()).compareTo(new Date(a.lastModified())));
            licenceModelFile = licenceModelFiles[0];
        } else {
            String[] assets = context.getAssets().list("");
            for (String asset : assets) {
                if (asset.startsWith("licence") || asset.startsWith("license")) {
                    try (InputStream inputStream = context.getAssets().open(asset)) {
                        licenceModelFile = new File(context.getFilesDir(), asset);
                        try (FileOutputStream outputStream = new FileOutputStream(licenceModelFile)) {
                            int read;
                            byte[] buffer = new byte[256];
                            while ((read = inputStream.read(buffer, 0, buffer.length)) > 0) {
                                outputStream.write(buffer, 0, read);
                            }
                            outputStream.flush();
                        }
                    }
                    break;
                }
            }
        }
        if (licenceModelFile != null) {
            return new Classifier("licence", licenceModelFile.getPath());
        } else {
            throw new IOException("Licence authenticity model file not found");
        }
    }
}
