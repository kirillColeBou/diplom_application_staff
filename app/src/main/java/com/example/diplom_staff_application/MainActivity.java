package com.example.diplom_staff_application;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String KEY_SELECTED_FILTER = "selected_filter";
    private String currentFilter = "В сборке";
    private PopupWindow popupWindow;
    private RecyclerView recyclerView;
    private OrderAdapter orderAdapter;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyOrderItem;
    private Button btnReceived;
    private Button btnCollected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.main_activity);
        recyclerView = findViewById(R.id.recycler_view_order);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        emptyOrderItem = findViewById(R.id.empty_order_item);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderAdapter(new ArrayList<>());
        recyclerView.setAdapter(orderAdapter);
        loadOrders();
        swipeRefresh.setOnRefreshListener(this::loadOrders);
        btnReceived = findViewById(R.id.btn_received);
        btnCollected = findViewById(R.id.btn_collected);
        if (savedInstanceState != null) {
            currentFilter = savedInstanceState.getString(KEY_SELECTED_FILTER, "В сборке");
        }
        if ("Ожидает получения".equals(currentFilter)) {
            btnReceived.setBackgroundColor(ContextCompat.getColor(this, R.color.inactive_button_color));
            btnCollected.setBackgroundColor(ContextCompat.getColor(this, R.color.active_button_color));
        } else {
            btnReceived.setBackgroundColor(ContextCompat.getColor(this, R.color.active_button_color));
            btnCollected.setBackgroundColor(ContextCompat.getColor(this, R.color.inactive_button_color));
        }
        loadOrdersByStatus(currentFilter);
        swipeRefresh.setOnRefreshListener(() -> loadOrdersByStatus(currentFilter));
    }

    private void loadOrders() {
        long storeId = AuthUtils.getCurrentStoreId(this);
        if (storeId == -1) {
            Toast.makeText(this, "Магазин не выбран", Toast.LENGTH_SHORT).show();
            swipeRefresh.setRefreshing(false);
            return;
        }
        OrderContext.loadStoreOrdersInProgress(storeId, new OrderContext.LoadOrdersCallback() {
            @Override
            public void onSuccess(List<Order> orders) {
                swipeRefresh.setRefreshing(false);
                orderAdapter.updateOrders(orders);

                if (orders.isEmpty()) {
                    emptyOrderItem.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    TextView emptyText = findViewById(R.id.empty_text);
                    emptyText.setText("Нет заказов в сборке");
                } else {
                    emptyOrderItem.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onOrderReceived(View view) {
        btnReceived.setBackgroundColor(ContextCompat.getColor(this, R.color.active_button_color));
        btnCollected.setBackgroundColor(ContextCompat.getColor(this, R.color.inactive_button_color));
        currentFilter = "В сборке";
        loadOrdersByStatus(currentFilter);
    }

    public void onOrderCollected(View view) {
        btnReceived.setBackgroundColor(ContextCompat.getColor(this, R.color.inactive_button_color));
        btnCollected.setBackgroundColor(ContextCompat.getColor(this, R.color.active_button_color));
        currentFilter = "Ожидает получения";
        loadOrdersByStatus(currentFilter);
    }

    private void loadOrdersByStatus(String status) {
        long storeId = AuthUtils.getCurrentStoreId(this);
        if (storeId == -1) {
            Toast.makeText(this, "Магазин не выбран", Toast.LENGTH_SHORT).show();
            swipeRefresh.setRefreshing(false);
            return;
        }

        OrderContext.loadStoreOrdersByStatus(storeId, status, new OrderContext.LoadOrdersCallback() {
            @Override
            public void onSuccess(List<Order> orders) {
                swipeRefresh.setRefreshing(false);
                orderAdapter.updateOrders(orders);
                if (orders.isEmpty()) {
                    emptyOrderItem.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    TextView emptyText = emptyOrderItem.findViewById(R.id.empty_text);
                    emptyText.setText(status.equals("В сборке") ?
                            "Нет заказов в сборке" : "Нет заказов, ожидающих получения");
                } else {
                    emptyOrderItem.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onMenu(View view) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.menu, null);
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.5);
        int height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
        popupWindow = new PopupWindow(popupView, width, height, true);
        popupWindow.setElevation(10);
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.menu_background));
        popupWindow.setOutsideTouchable(true);
        Button btnLogout = popupView.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            logout();
            popupWindow.dismiss();
        });
        popupWindow.showAsDropDown(view, 0, 0, Gravity.START);
    }

    private void logout() {
        AuthUtils.logout(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onDestroy() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SELECTED_FILTER, currentFilter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadOrdersByStatus(currentFilter);
        }
    }
}