package com.appliedrec.credentials.app;

import androidx.collection.ArraySet;

import com.appliedrec.barcodedatamatcher.AddressedDocumentMatcher;
import com.appliedrec.barcodedatamatcher.BarcodeMatching;
import com.appliedrec.barcodedatamatcher.DatedDocumentMatcher;
import com.appliedrec.barcodedatamatcher.MultiMatcher;
import com.appliedrec.barcodedatamatcher.NamedDocumentMatcher;
import com.appliedrec.barcodedatamatcher.NumberedDocumentMatcher;
import com.microblink.entities.recognizers.blinkid.generic.BlinkIdCombinedRecognizer;
import com.microblink.entities.recognizers.blinkid.generic.viz.VizResult;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

import io.reactivex.rxjava3.core.Single;

public class FrontBackMatcher {

    public Single<Float> getFrontBackMatchScore(BlinkIdCombinedRecognizer.Result result) {
        return Single.create(emitter -> {
            VizResult frontResult = result.getFrontVizResult();
            if (frontResult.isEmpty()) {
                emitter.onError(new Exception("Front page result is empty"));
                return;
            }
            if (result.getBarcodeResult().isEmpty()) {
                emitter.onError(new Exception("Barcode result is empty"));
                return;
            }
            Set<BarcodeMatching> matchers = new ArraySet<>();
            if (!frontResult.getFirstName().isEmpty() && !frontResult.getLastName().isEmpty()) {
                matchers.add(new NamedDocumentMatcher(frontResult.getFirstName(), frontResult.getLastName()));
            } else if (!frontResult.getFullName().isEmpty()) {
                matchers.add(new NumberedDocumentMatcher(frontResult.getFullName()));
            }
            if (!frontResult.getDocumentNumber().isEmpty()) {
                matchers.add(new NumberedDocumentMatcher(frontResult.getDocumentNumber()));
            }
            if (!frontResult.getAddress().isEmpty()) {
                matchers.add(new AddressedDocumentMatcher(frontResult.getAddress()));
            }
            if (frontResult.getDateOfIssue().getDate() != null && frontResult.getDateOfExpiry().getDate() != null && frontResult.getDateOfBirth().getDate() != null) {
                matchers.add(new DatedDocumentMatcher(microblinkDateToDate(frontResult.getDateOfIssue().getDate()), microblinkDateToDate(frontResult.getDateOfBirth().getDate()), microblinkDateToDate(frontResult.getDateOfExpiry().getDate())));
            }
            MultiMatcher documentMatcher = new MultiMatcher(matchers);
            float score = documentMatcher.matchBarcode(result.getBarcodeResult().getRawData());
            emitter.onSuccess(score);
        });
    }

    private Date microblinkDateToDate(com.microblink.results.date.Date microblinkDate) {
        GregorianCalendar calendar = new GregorianCalendar();
        int month = microblinkDate.getMonth();
        if (month != 0) {
            month -= 1;
        }
        int day = microblinkDate.getDay();
        if (day == 0) {
            day = 1;
        }
        calendar.set(microblinkDate.getYear(), month, day, 0, 0, 0);
        Date date = calendar.getTime();
        long time = date.getTime();
        if (time > 0) {
            time -= time % 1000;
        } else {
            time += time % 1000;
        }
        return new Date(time);
    }
}
