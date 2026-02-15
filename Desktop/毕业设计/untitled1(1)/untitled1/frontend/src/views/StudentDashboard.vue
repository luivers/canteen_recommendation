<template>
  <div class="student-dashboard-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">我的饮食数据</h1>
      <div class="header-actions">
        <el-button type="primary" @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 用户信息卡片 -->
    <div class="user-info-section">
      <el-card>
        <div class="user-info-content">
          <div class="user-avatar">
            <img :src="userAvatar" alt="用户头像" />
          </div>
          <div class="user-details">
            <h2>{{ userName }}</h2>
            <p class="user-role">{{ userRole }}</p>
            <div class="user-stats">
              <div class="stat-item">
                <span class="stat-value">{{ totalOrders }}</span>
                <span class="stat-label">总订单数</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">¥{{ totalSpending }}</span>
                <span class="stat-label">总消费</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ avgSpending }}</span>
                <span class="stat-label">平均消费</span>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 口味偏好分析 -->
    <div class="preferences-section">
      <el-card>
        <template #header>
          <span>口味偏好分析</span>
        </template>
        <div class="preferences-content">
          <div class="preference-item">
            <h3>最喜欢的品类</h3>
            <div class="preference-value">
              {{ preferences.favoriteCategory || "暂无数据" }}
            </div>
          </div>
          <div class="preference-item">
            <h3>偏好辣度</h3>
            <div class="preference-value">
              {{ preferences.spiceLevel || "暂无数据" }}
            </div>
          </div>
          <div class="preference-item">
            <h3>常消费时段</h3>
            <div class="preference-value">
              {{ preferences.frequentTime || "暂无数据" }}
            </div>
          </div>
          <div class="preference-item">
            <h3>最喜欢的菜品</h3>
            <div class="favorite-dishes">
              <el-tag
                v-for="dish in preferences.favoriteDishes"
                :key="dish"
                class="dish-tag"
                >{{ dish }}</el-tag
              >
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 饮食趋势分析 -->
    <div class="trends-section">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>消费趋势</span>
            </template>
            <div ref="spendingTrendRef" style="height: 300px"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>品类消费占比</span>
            </template>
            <div ref="categoryConsumptionRef" style="height: 300px"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 健康饮食建议 -->
    <div class="health-section">
      <el-card>
        <template #header>
          <span>健康饮食建议</span>
        </template>
        <div class="health-recommendations">
          <el-timeline>
            <el-timeline-item
              v-for="(recommendation, index) in healthRecommendations"
              :key="index"
              :timestamp="`评分: ${recommendation.score}/100`"
            >
              <div class="recommendation-content">
                <h4 class="recommendation-type">{{ recommendation.type }}</h4>
                <p class="recommendation-text">{{ recommendation.content }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </el-card>
    </div>

    <!-- 个性化推荐 -->
    <div class="recommendations-section">
      <el-card>
        <template #header>
          <span>智能推荐中心</span>
        </template>

        <el-tabs v-model="activeTab" @tab-click="handleTabClick">
          <el-tab-pane label="为您推荐" name="personalized"></el-tab-pane>
          <el-tab-pane label="新菜尝鲜" name="discovery"></el-tab-pane>
          <el-tab-pane label="减脂优选" name="weight_loss"></el-tab-pane>
          <el-tab-pane label="增肌搭档" name="muscle_gain"></el-tab-pane>
        </el-tabs>

        <div v-loading="loading" class="personalized-recommendations">
          <el-empty
            v-if="recommendedDishes.length === 0"
            description="暂无推荐菜品"
          />
          <div v-else class="dishes-grid">
            <el-card
              v-for="dish in recommendedDishes"
              :key="dish.id"
              class="dish-card"
            >
              <div class="dish-image">
                <img
                  :src="dish.image || '/default-avatar.png'"
                  alt="菜品图片"
                />
              </div>
              <div class="dish-info">
                <h3 class="dish-name">{{ dish.name }}</h3>
                <p class="dish-price">¥{{ dish.price }}</p>
                <el-rate :value="dish.rating || 0" disabled />
              </div>
            </el-card>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { ElMessage } from "element-plus";
import * as echarts from "echarts";
import { Refresh } from "@element-plus/icons-vue";
import { statisticsApi } from "@/api/statistics";

// 用户信息
const userAvatar = ref("/default-avatar.png");
const userName = ref("张三");
const userRole = ref("学生");
const totalOrders = ref(0);
const totalSpending = ref(0);
const avgSpending = ref(0);

// 口味偏好
const preferences = ref({
  favoriteCategory: "",
  favoriteDishes: [],
  spiceLevel: "",
  averageSpending: 0,
  frequentTime: "",
});

// 健康饮食建议
const healthRecommendations = ref([]);

// 推荐菜品
const recommendedDishes = ref([]);
const activeTab = ref("personalized");
const loading = ref(false);

const handleTabClick = () => {
  loadRecommendedDishes();
};

// 图表引用
const spendingTrendRef = ref(null);
const categoryConsumptionRef = ref(null);

let spendingTrendChart = null;
let categoryConsumptionChart = null;

// 初始化图表
const initCharts = () => {
  // 消费趋势图
  spendingTrendChart = echarts.init(
    spendingTrendRef.value || document.createElement("div"),
  );
  spendingTrendChart.setOption({
    tooltip: {
      trigger: "axis",
      formatter: "{b}: ¥{c}",
    },
    xAxis: {
      type: "category",
      data: [],
      axisLabel: {
        interval: 0,
      },
    },
    yAxis: {
      type: "value",
      name: "消费金额（元）",
    },
    series: [
      {
        data: [],
        type: "line",
        smooth: true,
        areaStyle: {
          color: {
            type: "linear",
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              {
                offset: 0,
                color: "rgba(64, 158, 255, 0.3)",
              },
              {
                offset: 1,
                color: "rgba(64, 158, 255, 0.1)",
              },
            ],
          },
        },
        itemStyle: {
          color: "#409EFF",
        },
      },
    ],
  });

  // 品类消费占比图
  categoryConsumptionChart = echarts.init(
    categoryConsumptionRef.value || document.createElement("div"),
  );
  categoryConsumptionChart.setOption({
    tooltip: {
      trigger: "item",
      formatter: "{b}: {c}元 ({d}%)",
    },
    legend: {
      orient: "vertical",
      left: 10,
      data: [],
    },
    series: [
      {
        name: "品类消费",
        type: "pie",
        radius: "50%",
        data: [],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: "rgba(0, 0, 0, 0.5)",
          },
        },
      },
    ],
  });
};

// 更新消费趋势图
const updateSpendingTrendChart = () => {
  // 模拟数据
  const dates = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"];
  const spendingData = [25, 30, 20, 35, 28, 40, 32];

  if (spendingTrendChart) {
    spendingTrendChart.setOption({
      xAxis: {
        data: dates,
      },
      series: [
        {
          data: spendingData,
        },
      ],
    });
  }
};

// 更新品类消费占比图
const updateCategoryConsumptionChart = () => {
  // 模拟数据
  const categories = [
    { name: "川菜", value: 120 },
    { name: "粤菜", value: 80 },
    { name: "湘菜", value: 60 },
    { name: "鲁菜", value: 40 },
    { name: "其他", value: 30 },
  ];

  if (categoryConsumptionChart) {
    const categoryNames = categories.map((item) => item.name);

    categoryConsumptionChart.setOption({
      legend: {
        data: categoryNames,
      },
      series: [
        {
          data: categories,
        },
      ],
    });
  }
};

// 加载用户偏好数据
const loadUserPreferences = async () => {
  try {
    // 这里应该从API获取用户ID
    const userId = 1;
    const response = await statisticsApi.getUserPreferences(userId);
    preferences.value = response.data.preferences || {};

    // 更新用户统计数据
    totalOrders.value = Math.floor(Math.random() * 100) + 10;
    totalSpending.value = Math.floor(Math.random() * 2000) + 500;
    avgSpending.value = preferences.value.averageSpending || 25.5;
  } catch (error) {
    console.error("加载用户偏好失败:", error);
    ElMessage.error("加载用户偏好失败");
  }
};

// 加载健康饮食建议
const loadHealthRecommendations = async () => {
  try {
    // 这里应该从API获取用户ID
    const userId = 1;
    const response = await statisticsApi.getHealthRecommendations(userId);
    healthRecommendations.value = response.data || [];
  } catch (error) {
    console.error("加载健康建议失败:", error);
    ElMessage.error("加载健康建议失败");
  }
};

// 加载推荐菜品
const loadRecommendedDishes = async () => {
  loading.value = true;
  try {
    let response;
    switch (activeTab.value) {
      case "personalized":
        response = await recommendationApi.getPersonalizedRecommendations(4);
        break;
      case "discovery":
        response = await recommendationApi.getDiscoveryRecommendations(4);
        break;
      case "context":
        response = await recommendationApi.getRecommendationsByStrategy(
          "context",
          4,
        );
        break;
      case "weight_loss":
        response = await recommendationApi.getHealthRecommendations(
          "weight_loss",
          4,
        );
        break;
      case "muscle_gain":
        response = await recommendationApi.getHealthRecommendations(
          "muscle_gain",
          4,
        );
        break;
      default:
        response = await recommendationApi.getPersonalizedRecommendations(4);
    }

    // 处理响应数据格式，使其适配当前页面
    let data = [];
    if (response && response.data && Array.isArray(response.data)) {
      data = response.data;
    } else if (response && Array.isArray(response)) {
      data = response;
    }

    recommendedDishes.value = data.map((dish) => ({
      id: dish.id,
      name: dish.name,
      price: dish.price,
      rating: dish.rating || 5.0, // 默认评分
      image: dish.image || "/dishes/main_dish.svg", // 默认图片
    }));
  } catch (error) {
    console.error("加载推荐菜品失败:", error);
    // 失败时保持空数组或显示错误
    recommendedDishes.value = [];
  } finally {
    loading.value = false;
  }
};

// 加载数据
const loadData = async () => {
  await Promise.all([
    loadUserPreferences(),
    loadHealthRecommendations(),
    loadRecommendedDishes(),
  ]);

  // 更新图表
  updateSpendingTrendChart();
  updateCategoryConsumptionChart();
};

// 刷新数据
const refreshData = () => {
  loadData();
  ElMessage.success("数据已刷新");
};

// 窗口大小变化时重绘图表
const handleResize = () => {
  if (spendingTrendChart) spendingTrendChart.resize();
  if (categoryConsumptionChart) categoryConsumptionChart.resize();
};

onMounted(() => {
  // 初始化用户信息
  userName.value = localStorage.getItem("userName") || "学生用户";
  userRole.value = localStorage.getItem("userRole") || "STUDENT";

  // 初始化图表
  initCharts();
  // 加载数据
  loadData();
  // 监听窗口大小变化
  window.addEventListener("resize", handleResize);
});

onUnmounted(() => {
  window.removeEventListener("resize", handleResize);
  if (spendingTrendChart) spendingTrendChart.dispose();
  if (categoryConsumptionChart) categoryConsumptionChart.dispose();
});
</script>

<style scoped>
.student-dashboard-container {
  width: 100%;
  margin: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-title {
  font-size: 24px;
  color: #333;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.user-info-section {
  margin-bottom: 30px;
}

.user-info-content {
  display: flex;
  align-items: center;
  padding: 20px;
}

.user-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 30px;
  border: 3px solid #409eff;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-details h2 {
  margin: 0 0 5px 0;
  font-size: 24px;
  color: #333;
}

.user-role {
  margin: 0 0 20px 0;
  color: #666;
  font-size: 14px;
}

.user-stats {
  display: flex;
  gap: 40px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 5px;
}

.stat-label {
  display: block;
  font-size: 14px;
  color: #666;
}

.preferences-section {
  margin-bottom: 30px;
}

.preferences-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  padding: 20px 0;
}

.preference-item h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #666;
}

.preference-value {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.favorite-dishes {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.dish-tag {
  margin-bottom: 5px;
}

.trends-section {
  margin-bottom: 30px;
}

.health-section {
  margin-bottom: 30px;
}

.health-recommendations {
  padding: 20px 0;
}

.recommendation-type {
  margin: 0 0 5px 0;
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.recommendation-text {
  margin: 0;
  color: #666;
  line-height: 1.6;
}

.recommendations-section {
  margin-bottom: 30px;
}

.personalized-recommendations {
  padding: 20px 0;
}

.dishes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}

.dish-card {
  transition: transform 0.3s ease;
}

.dish-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.dish-image {
  height: var(--dish-image-size);
  overflow: hidden;
  border-radius: 8px 8px 0 0;
}

.dish-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.dish-card:hover .dish-image img {
  transform: scale(1.1);
}

.dish-info {
  padding: 15px;
}

.dish-name {
  margin: 0 0 10px 0;
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.dish-price {
  margin: 0 0 10px 0;
  font-size: 20px;
  font-weight: bold;
  color: #f56c6c;
}
</style>
