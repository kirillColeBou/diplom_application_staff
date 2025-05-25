package com.example.diplom_staff_application;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class OrderInfoActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private OrderItemsAdapter adapter;
    private TextView orderNumberText;
    private SwipeRefreshLayout swipeRefresh;
    private View btnOrderComplete;
    private long orderId;
    private String currentStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.order_info_activity);
        orderId = getIntent().getLongExtra("orderId", -1);
        if (orderId == -1) {
            Toast.makeText(this, "Ошибка: заказ не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        initViews();
        loadOrderItems();
    }

    private void initViews() {
        orderNumberText = findViewById(R.id.order_number_text);
        orderNumberText.setText(String.format("Заказ №%d", orderId));
        btnOrderComplete = findViewById(R.id.btn_order_complete);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadOrderItems);
        recyclerView = findViewById(R.id.recycler_view_order_info);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderItemsAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
    }

    private void loadOrderItems() {
        OrderContext.loadOrderItems(orderId, new OrderContext.LoadOrderItemsCallback() {
            @Override
            public void onSuccess(List<CartItem> items, String status) {
                swipeRefresh.setRefreshing(false);
                currentStatus = status;
                adapter.updateItems(items);
                btnOrderComplete.setVisibility("В сборке".equals(status) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String error) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(OrderInfoActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onBack(View view) {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    public void onOrderComplete(View view) {
        if (!"В сборке".equals(currentStatus)) {
            Toast.makeText(this, "Нельзя изменить статус этого заказа", Toast.LENGTH_SHORT).show();
            return;
        }
        OrderContext.updateOrderStatus(orderId, "Ожидает получения", new OrderContext.UpdateOrderCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(OrderInfoActivity.this, "Статус заказа обновлен", Toast.LENGTH_SHORT).show();
                currentStatus = "Ожидает получения";
                btnOrderComplete.setVisibility(View.GONE);
                loadOrderItems();
                setResult(RESULT_OK);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(OrderInfoActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}