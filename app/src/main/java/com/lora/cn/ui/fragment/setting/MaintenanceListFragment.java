package com.lora.cn.ui.fragment.setting;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.ui.adapter.MaintenanceInfoAdapter;
import com.lora.cn.ui.model.MaintenanceInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MaintenanceListFragment extends Fragment {

    private RecyclerView rv;
    private MaintenanceInfoAdapter adapter;
    private DatabaseHelper db;
    private long currentUserId = -1;
    private String currentUserName = "";

    public static MaintenanceListFragment newInstance() {
        return new MaintenanceListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_maintenance_list, container, false);
        rv = v.findViewById(R.id.rv_maintenance);
        View btnAdd = v.findViewById(R.id.btn_add_maintenance);
        View btnBack = v.findViewById(R.id.back);

        db = DatabaseHelper.getInstance(requireContext());
        currentUserId = SPUtils.getInstance().getLong("current_user_id", -1);
        currentUserName = SPUtils.getInstance().getString("current_user_name", "");

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MaintenanceInfoAdapter();
        adapter.setOnConfirmClickListener(this::showConfirmDialog);
        adapter.setOnViewClickListener(this::showViewDialog);
        rv.setAdapter(adapter);

        if (btnBack != null) {
            btnBack.setOnClickListener(view -> {
                try {
                    getParentFragmentManager().popBackStack();
                } catch (Exception ignored) {}
            });
        }
        if (btnAdd != null) {
            btnAdd.setOnClickListener(view -> showAddDialog());
        }

        loadList();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadList();
    }

    private void loadList() {
        try {
            List<MaintenanceInfo> list = db.getMaintenanceRecords(currentUserId);
            if (list == null) list = new ArrayList<>();
            adapter.submitList(list);
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "加载维护列表失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddDialog() {
        List<com.lora.cn.ui.model.Terminal> loaded;
        try {
            loaded = db.getAllTerminals();
        } catch (Exception e) {
            loaded = null;
        }
        final List<com.lora.cn.ui.model.Terminal> terminals = (loaded != null) ? loaded : new ArrayList<>();
        if (terminals.isEmpty()) {
            Toast.makeText(requireContext(), "暂无终端可选", Toast.LENGTH_SHORT).show();
            return;
        }

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_maintenance, null);
        Spinner spinner = view.findViewById(R.id.spinner_terminal);
        EditText etContent = view.findViewById(R.id.et_content);

        List<String> display = new ArrayList<>();
        for (com.lora.cn.ui.model.Terminal t : terminals) {
            String id = t.getTerminalId() == null ? "" : t.getTerminalId();
            String name = t.getTerminalName() == null ? "" : t.getTerminalName();
            display.add(id + " - " + name);
        }
        android.widget.ArrayAdapter<String> spinAdapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, display);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinAdapter);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("新增维护")
                .setView(view)
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null)
                .create();

        dialog.setOnShowListener(dlg -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int idx = spinner.getSelectedItemPosition();
            if (idx < 0 || idx >= terminals.size()) {
                Toast.makeText(requireContext(), "请选择终端", Toast.LENGTH_SHORT).show();
                return;
            }
            String content = etContent != null ? etContent.getText().toString().trim() : "";
            if (TextUtils.isEmpty(content)) {
                Toast.makeText(requireContext(), "请输入维护内容", Toast.LENGTH_SHORT).show();
                return;
            }
            com.lora.cn.ui.model.Terminal t = terminals.get(idx);
            MaintenanceInfo mi = new MaintenanceInfo();
            mi.setTerminalId(t.getTerminalId());
            mi.setTerminalName(t.getTerminalName());
            String group = t.getDepartment();
            if (TextUtils.isEmpty(group)) group = t.getLocation();
            mi.setTerminalGroup(group);
            mi.setStatus(0);
            mi.setContent(content);
            mi.setCreateUserId(currentUserId);
            mi.setCreateUser(currentUserName);
            mi.setCreateTime(nowStr());
            mi.setHandleUserId(0);
            mi.setHandleUser("");
            mi.setHandleTime("");
            long r = db.addMaintenanceRecord(mi);
            if (r > 0) {
                dialog.dismiss();
                loadList();
                Toast.makeText(requireContext(), "已添加", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "添加失败", Toast.LENGTH_SHORT).show();
            }
        }));
        dialog.show();
    }

    private void showConfirmDialog(MaintenanceInfo item) {
        if (item == null) return;
        EditText et = new EditText(requireContext());
        et.setMinLines(3);
        et.setText(item.getContent() == null ? "" : item.getContent());
        et.setSelection(et.getText().length());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("确认维护")
                .setView(et)
                .setPositiveButton("确认", (d, w) -> {
                    String content = et.getText() != null ? et.getText().toString().trim() : "";
                    int r = db.updateMaintenanceHandled(item.getId(), currentUserId, currentUserName, nowStr(), content);
                    if (r > 0) {
                        loadList();
                        Toast.makeText(requireContext(), "已确认维护", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "操作失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        dialog.show();
    }

    private void showViewDialog(MaintenanceInfo item) {
        if (item == null) return;
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(item.getContent())) sb.append(item.getContent());
        if (!TextUtils.isEmpty(item.getHandleUser()) || !TextUtils.isEmpty(item.getHandleTime())) {
            if (sb.length() > 0) sb.append("\n\n");
            if (!TextUtils.isEmpty(item.getHandleUser())) sb.append("维护人：").append(item.getHandleUser()).append("\n");
            if (!TextUtils.isEmpty(item.getHandleTime())) sb.append("维护时间：").append(item.getHandleTime());
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("维护内容")
                .setMessage(sb.toString())
                .setPositiveButton("确定", null)
                .create()
                .show();
    }

    private String nowStr() {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}
