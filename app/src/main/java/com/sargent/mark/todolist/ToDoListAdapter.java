package com.sargent.mark.todolist;

import android.view.LayoutInflater;
import android.widget.Toast;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.sargent.mark.todolist.data.Contract;

/**
 * Created by mark on 7/4/17.
 */

public class ToDoListAdapter extends RecyclerView.Adapter<ToDoListAdapter.ItemHolder> {

    // Instantiate a SQLite Database to perform commands
    private SQLiteDatabase database;
    private Cursor cursor;
    private ItemClickListener listener;
    private String TAG = "todolistadapter";

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.item, parent, false);
        ItemHolder holder = new ItemHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ItemHolder holder, int position) {

        holder.bind(holder, position);
    }

    @Override
    public int getItemCount() {

        return cursor.getCount();
    }


    public interface ItemClickListener {
        // Must pass in the category
        void onItemClick(int pos, String description, String duedate, long id, String category);
    }

    public ToDoListAdapter(Cursor cursor, ItemClickListener listener, SQLiteDatabase database) {
        this.cursor = cursor;
        this.listener = listener;

        // Set database to change as well
        this.database = database;
    }

    public void swapCursor(Cursor newCursor){
        if (cursor != null) cursor.close();
        cursor = newCursor;
        if (newCursor != null) {
            // Force the RecyclerView to refresh
            this.notifyDataSetChanged();
        }
    }

    class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // Instantiate checkbox and category to store the relevant category
        CheckBox checkBox;
        String category;
        TextView descr;
        TextView due;
        String duedate;
        String description;
        long id;

        View view;

        ItemHolder(View view) {
            super(view);
            descr = (TextView) view.findViewById(R.id.description);
            due = (TextView) view.findViewById(R.id.dueDate);

            // ASsign the checkbox previously instantiated
            checkBox = (CheckBox) view.findViewById(R.id.checkBox);

            this.view = view;

            view.setOnClickListener(this);
        }

        public void bind(ItemHolder holder, int pos) {
            cursor.moveToPosition(pos);
            id = cursor.getLong(cursor.getColumnIndex(Contract.TABLE_TODO._ID));
            Log.d(TAG, "deleting id: " + id);

            duedate = cursor.getString(cursor.getColumnIndex(Contract.TABLE_TODO.COLUMN_NAME_DUE_DATE));
            description = cursor.getString(cursor.getColumnIndex(Contract.TABLE_TODO.COLUMN_NAME_DESCRIPTION));
            descr.setText(description);
            due.setText(duedate);
            holder.itemView.setTag(id);


            // Save the category as well when interacting with the database
            category = cursor.getString(cursor.getColumnIndex(Contract.TABLE_TODO.COLUMN_NAME_CATEGORY));


            // Checks to see if the selected event is Completed
            // If 1 completed
            // If 0 not completed
            if(cursor.getInt(cursor.getColumnIndex(Contract.TABLE_TODO.COLUMN_NAME_EVENTCOMPLETED)) == 1){
                checkBox.setChecked(true);
            }
            else{
                checkBox.setChecked(false);
            }

            // Update database
            checkBox.setOnClickListener( new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    if (checkBox.isChecked()){
                        updateEvent(true);
                    }
                    else{
                        updateEvent(false);
                    }
                }
            });

        }

        @Override
        public void onClick(View v) {
            int pos = getAdapterPosition();
            listener.onItemClick(pos, description, duedate, id, category);
        }

        public void updateEvent(boolean completed){
            ContentValues cv = new ContentValues();
            //Checks completion of events and updates database accordingly
            if(completed) {
                cv.put(Contract.TABLE_TODO.COLUMN_NAME_EVENTCOMPLETED, 1);
            }
            else{
                cv.put(Contract.TABLE_TODO.COLUMN_NAME_EVENTCOMPLETED, 0);
            }

            database.update(Contract.TABLE_TODO.TABLE_NAME, cv, Contract.TABLE_TODO._ID + "=" + id, null);
        }
    }

}
