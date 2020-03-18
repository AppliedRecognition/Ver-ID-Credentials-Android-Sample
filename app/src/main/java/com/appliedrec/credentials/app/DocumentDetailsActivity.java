package com.appliedrec.credentials.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appliedrec.credentials.app.databinding.ActivityDocumentDetailsBinding;
import com.appliedrec.credentials.app.databinding.ListItemDocPropertyBinding;
import com.appliedrec.rxverid.RxVerIDActivity;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;

public class DocumentDetailsActivity extends RxVerIDActivity {

    static class DocumentPropertyViewHolder extends RecyclerView.ViewHolder {

        private final ListItemDocPropertyBinding viewBinding;

        DocumentPropertyViewHolder(@NonNull ListItemDocPropertyBinding viewBinding) {
            super(viewBinding.getRoot());
            this.viewBinding = viewBinding;
        }

        void bind(String name, String value) {
            viewBinding.key.setText(name);
            viewBinding.value.setText(value);
        }
    }

    static class DocumentDetailsAdapter extends RecyclerView.Adapter<DocumentPropertyViewHolder> {

        private final LayoutInflater layoutInflater;
        private final List<Pair<String,String>> documentProperties;

        DocumentDetailsAdapter(Context context, List<Pair<String,String>> documentProperties) {
            layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.documentProperties = documentProperties;
        }

        @NonNull
        @Override
        public DocumentPropertyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ListItemDocPropertyBinding viewBinding = ListItemDocPropertyBinding.inflate(layoutInflater, parent, false);
            return new DocumentPropertyViewHolder(viewBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull DocumentPropertyViewHolder holder, int position) {
            Pair<String,String> pair = documentProperties.get(position);
            holder.bind(pair.first, pair.second);
        }

        @Override
        public int getItemCount() {
            return documentProperties.size();
        }
    }

    private ActivityDocumentDetailsBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityDocumentDetailsBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        Intent intent = getIntent();
        if (intent != null) {
            DocumentData documentData = intent.getParcelableExtra(IDCardActivity.EXTRA_DOCUMENT_DATA);
            if (documentData != null) {
                if (documentData.getRawBarcode() != null) {
                    try {
                        SecureStorage secureStorage = new SecureStorage(this);
                        String intellicheckSecret = secureStorage.getValueForKey();
                        if (intellicheckSecret != null) {
                            IntellicheckBarcodeVerifier barcodeVerifier = new IntellicheckBarcodeVerifier(this, intellicheckSecret);
                            addDisposable(barcodeVerifier.parseBarcode(documentData.getRawBarcode()).toList().observeOn(AndroidSchedulers.mainThread()).subscribe(
                                    this::showProperties,
                                    error -> {
                                        showProperties(propertiesFromDocumentData(documentData));
                                        new AlertDialog.Builder(this)
                                                .setTitle(R.string.error)
                                                .setMessage(R.string.failed_to_verify_barcode)
                                                .setNeutralButton(android.R.string.ok, null)
                                                .create()
                                                .show();
                                    }
                            ));
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                showProperties(propertiesFromDocumentData(documentData));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewBinding = null;
    }

    private List<Pair<String,String>> propertiesFromDocumentData(DocumentData documentData) {
        ArrayList<Pair<String,String>> properties = new ArrayList<>();
        if (documentData.getFirstName() != null && !documentData.getFirstName().isEmpty()) {
            properties.add(new Pair<>(getString(R.string.first_name), documentData.getFirstName()));
        }
        if (documentData.getLastName() != null && !documentData.getLastName().isEmpty()) {
            properties.add(new Pair<>(getString(R.string.last_name), documentData.getLastName()));
        }
        if (documentData.getDateOfBirth() != null && !documentData.getDateOfBirth().isEmpty()) {
            properties.add(new Pair<>(getString(R.string.date_of_birth), documentData.getDateOfBirth()));
        }
        if (documentData.getSex() != null && !documentData.getSex().isEmpty()) {
            properties.add(new Pair<>(getString(R.string.sex), documentData.getSex()));
        }
        if (documentData.getAddress() != null && !documentData.getAddress().isEmpty()) {
            properties.add(new Pair<>(getString(R.string.address), documentData.getAddress()));
        }
        if (documentData.getDateOfIssue() != null && !documentData.getDateOfIssue().isEmpty()) {
            properties.add(new Pair<>(getString(R.string.date_of_issue), documentData.getDateOfIssue()));
        }
        if (documentData.getDateOfExpiry() != null && !documentData.getDateOfExpiry().isEmpty()) {
            properties.add(new Pair<>(getString(R.string.date_of_expiry), documentData.getDateOfExpiry()));
        }
        if (documentData.getDocumentNumber() != null && !documentData.getDocumentNumber().isEmpty()) {
            properties.add(new Pair<>(getString(R.string.document_number), documentData.getDocumentNumber()));
        }
        return properties;
    }

    private void showProperties(List<Pair<String,String>> properties) {
        viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        viewBinding.recyclerView.setHasFixedSize(true);
        DocumentDetailsAdapter adapter = new DocumentDetailsAdapter(this, properties);
        viewBinding.recyclerView.setAdapter(adapter);
    }
}
