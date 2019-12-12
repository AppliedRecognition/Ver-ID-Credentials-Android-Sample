package com.appliedrec.credentials.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DocumentDetailsActivity extends AppCompatActivity {

    static class DocumentPropertyViewHolder extends RecyclerView.ViewHolder {

        private TextView keyTextView;
        private TextView valueTextView;

        public DocumentPropertyViewHolder(@NonNull View itemView) {
            super(itemView);
            keyTextView = itemView.findViewById(R.id.key);
            valueTextView = itemView.findViewById(R.id.value);
        }

        void bind(String name, String value) {
            keyTextView.setText(name);
            valueTextView.setText(value);
        }
    }

    static class DocumentDetailsAdapter extends RecyclerView.Adapter<DocumentPropertyViewHolder> {

        private LayoutInflater layoutInflater;
        private ArrayList<Pair<String,String>> documentProperties;

        DocumentDetailsAdapter(Context context, ArrayList<Pair<String,String>> documentProperties) {
            layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.documentProperties = documentProperties;
        }

        @NonNull
        @Override
        public DocumentPropertyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = layoutInflater.inflate(R.layout.list_item_doc_property, parent, false);
            return new DocumentPropertyViewHolder(view);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_details);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        Intent intent = getIntent();
        if (intent != null) {
            DocumentData documentData = intent.getParcelableExtra(IDCardActivity.EXTRA_DOCUMENT_DATA);
            if (documentData != null) {
                ArrayList<Pair<String,String>> properties = new ArrayList<>();
                if (documentData.getFirstName() != null && !documentData.getFirstName().isEmpty()) {
                    properties.add(new Pair<>("First name", documentData.getFirstName()));
                }
                if (documentData.getLastName() != null && !documentData.getLastName().isEmpty()) {
                    properties.add(new Pair<>("Last name", documentData.getLastName()));
                }
                if (documentData.getDateOfBirth() != null && !documentData.getDateOfBirth().isEmpty()) {
                    properties.add(new Pair<>("Date of birth", documentData.getDateOfBirth()));
                }
                if (documentData.getSex() != null && !documentData.getSex().isEmpty()) {
                    properties.add(new Pair<>("Sex", documentData.getSex()));
                }
                if (documentData.getAddress() != null && !documentData.getAddress().isEmpty()) {
                    properties.add(new Pair<>("Address", documentData.getAddress()));
                }
                if (documentData.getDateOfIssue() != null && !documentData.getDateOfIssue().isEmpty()) {
                    properties.add(new Pair<>("Date of issue", documentData.getDateOfIssue()));
                }
                if (documentData.getDateOfExpiry() != null && !documentData.getDateOfExpiry().isEmpty()) {
                    properties.add(new Pair<>("Date of expiry", documentData.getDateOfExpiry()));
                }
                if (documentData.getDocumentNumber() != null && !documentData.getDocumentNumber().isEmpty()) {
                    properties.add(new Pair<>("Document number", documentData.getDocumentNumber()));
                }
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setHasFixedSize(true);
                DocumentDetailsAdapter adapter = new DocumentDetailsAdapter(this, properties);
                recyclerView.setAdapter(adapter);
            }
        }
    }
}
