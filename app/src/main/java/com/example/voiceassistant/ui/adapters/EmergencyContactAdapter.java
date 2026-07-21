package com.example.voiceassistant.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voiceassistant.R;
import com.example.voiceassistant.data.database.entity.EmergencyContact;

import java.util.ArrayList;
import java.util.List;

public class EmergencyContactAdapter extends RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder> {

    public interface OnContactClickListener {
        void onContactClick(EmergencyContact contact);
    }

    public interface OnContactDeleteClickListener {
        void onContactDeleteClick(EmergencyContact contact);
    }

    private final List<EmergencyContact> contacts = new ArrayList<>();
    private final OnContactClickListener contactClickListener;
    private final OnContactDeleteClickListener deleteClickListener;

    public EmergencyContactAdapter(
            OnContactClickListener contactClickListener,
            OnContactDeleteClickListener deleteClickListener
    ) {
        this.contactClickListener = contactClickListener;
        this.deleteClickListener = deleteClickListener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        EmergencyContact contact = contacts.get(position);

        if (contact.getPriority() == 1) {
            holder.tvPriorityBadge.setText(R.string.call_first_during_sos);
        } else {
            holder.tvPriorityBadge.setText(
                    holder.itemView.getContext().getString(R.string.contact_priority_number, contact.getPriority())
            );
        }
        holder.tvContactName.setText(contact.getName());
        holder.tvPhoneNumber.setText(contact.getPhoneNumber());
        holder.itemView.setContentDescription(
                holder.itemView.getContext().getString(
                        R.string.emergency_contact_card_content_description,
                        contact.getPriority(),
                        contact.getName(),
                        contact.getPhoneNumber()
                )
        );

        if (contact.getPriority() == 1) {
            holder.tvRelationship.setText(R.string.called_first_during_sos);
            holder.tvRelationship.setVisibility(View.VISIBLE);
        } else {
            holder.tvRelationship.setVisibility(View.GONE);
        }

        holder.btnEditContact.setContentDescription(
                holder.itemView.getContext().getString(R.string.edit_contact_named, contact.getName())
        );
        holder.btnEditContact.setEnabled(contactClickListener != null);
        holder.btnEditContact.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && contactClickListener != null) {
                contactClickListener.onContactClick(contacts.get(adapterPosition));
            }
        });

        holder.btnDeleteContact.setContentDescription(
                holder.itemView.getContext().getString(R.string.delete_contact_named, contact.getName())
        );
        holder.btnDeleteContact.setEnabled(deleteClickListener != null);
        holder.btnDeleteContact.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && deleteClickListener != null) {
                deleteClickListener.onContactDeleteClick(contacts.get(adapterPosition));
            }
        });
        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && contactClickListener != null) {
                contactClickListener.onContactClick(contacts.get(adapterPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void submitList(List<EmergencyContact> newContacts) {
        contacts.clear();
        if (newContacts != null) {
            contacts.addAll(newContacts);
        }
        notifyDataSetChanged();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView tvPriorityBadge;
        TextView tvContactName;
        TextView tvPhoneNumber;
        TextView tvRelationship;
        ImageButton btnEditContact;
        ImageButton btnDeleteContact;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPriorityBadge = itemView.findViewById(R.id.tv_priority_badge);
            tvContactName = itemView.findViewById(R.id.tv_contact_name);
            tvPhoneNumber = itemView.findViewById(R.id.tv_phone_number);
            tvRelationship = itemView.findViewById(R.id.tv_relationship);
            btnEditContact = itemView.findViewById(R.id.btn_edit_contact);
            btnDeleteContact = itemView.findViewById(R.id.btn_delete_contact);
        }
    }
}
