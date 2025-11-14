package com.lora.cn.ui.adapter;

import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.Category;
import com.lora.cn.database.entity.Group;
import com.lora.cn.ui.model.Terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DynamicGroupAdapter extends BaseQuickAdapter<Group, QuickViewHolder> {

    private final DatabaseManager dbManager;
    private final Map<Long, Long> selectedByGroup;
    private final Terminal terminal;

    public DynamicGroupAdapter(DatabaseManager dbManager, Map<Long, Long> selectedByGroup, Terminal terminal) {
        this.dbManager = dbManager;
        this.selectedByGroup = selectedByGroup;
        this.terminal = terminal;
    }

    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int i, @Nullable Group item) {
        TextView tv = holder.getView(R.id.item_device_title);
        Spinner sp = holder.getView(R.id.item_device_content);

        tv.setText(item.getGroupName());

        List<Category> cats = dbManager.getCategoriesByGroupId(item.getGroupId());
        List<String> names = new ArrayList<>();
        if (cats != null) {
            for (Category c : cats) names.add(c.getCategoryName());
        }
        if (names.isEmpty()) {
            names.add("暂无分类");
        }
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(holder.itemView.getContext(), android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
        sp.setTag(cats);
        sp.setEnabled(cats != null && !cats.isEmpty());

        long wantId = 0L;
        try {
            if (terminal != null) {
                long gid = item.getGroupId();
                if (gid == 1L) wantId = terminal.getDepartmentId();
                else if (gid == 2L) wantId = terminal.getRoomId();
                else if (gid == 3L) wantId = terminal.getNursingGroupId();
                else if (gid == 4L) wantId = terminal.getOtherId();
                else {
                    String ext = terminal.getExtension();
                    if (ext != null && !ext.isEmpty()) {
                        org.json.JSONObject obj = new org.json.JSONObject(ext);
                        if (obj.has("extra_groups")) {
                            org.json.JSONObject ex = obj.getJSONObject("extra_groups");
                            if (ex.has(String.valueOf(gid))) {
                                wantId = ex.optLong(String.valueOf(gid), 0L);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        if (wantId > 0L && cats != null && !cats.isEmpty()) {
            int idx = 0;
            for (int k = 0; k < cats.size(); k++) {
                if (cats.get(k).getCategoryId() == wantId) { idx = k; break; }
            }
            try { sp.setSelection(idx, false); } catch (Throwable ignored) {}
            selectedByGroup.put(item.getGroupId(), wantId);
        }

        sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Object tag = parent.getTag();
                if (tag instanceof List) {
                    List<Category> list = (List<Category>) tag;
                    if (position >= 0 && position < list.size()) {
                        selectedByGroup.put(item.getGroupId(), list.get(position).getCategoryId());
                    }
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull android.content.Context context, @NonNull android.view.ViewGroup viewGroup, int i) {
        return new QuickViewHolder(R.layout.item_add_device, viewGroup);
    }
}

