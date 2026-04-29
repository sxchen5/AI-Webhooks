<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <span>Git仓库扫描 · 下发任务日志</span>
        <el-button @click="load">刷新</el-button>
      </div>
    </template>
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="jobName" label="任务" min-width="110" />
      <el-table-column prop="repoName" label="仓库" min-width="100" />
      <el-table-column prop="triggerType" label="触发" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.triggerType === 'MANUAL'" type="primary">手动</el-tag>
          <el-tag v-else-if="row.triggerType === 'SCHEDULE'" type="warning">定时</el-tag>
          <span v-else>{{ row.triggerType }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="branch" label="分支" width="100" show-overflow-tooltip />
      <el-table-column prop="commitHash" label="Commit" min-width="120" show-overflow-tooltip />
      <el-table-column prop="execStatus" label="执行" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.execStatus === 1" type="success">成功</el-tag>
          <el-tag v-else-if="row.execStatus === 2" type="danger">失败</el-tag>
          <el-tag v-else type="info">进行中</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="emailStatus" label="邮件" width="100">
        <template #default="{ row }">
          <span v-if="row.emailStatus === 0">未发送</span>
          <span v-else-if="row.emailStatus === 1" style="color: #67c23a">已发送</span>
          <span v-else-if="row.emailStatus === 2" style="color: #f56c6c">失败</span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="taskStartTime" label="开始时间" width="170" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        v-model:current-page="page"
        :page-size="size"
        @current-change="load"
      />
    </div>
  </el-card>

  <el-drawer v-model="drawerVisible" title="下发扫描详情" size="62%">
    <template v-if="detail">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="任务">{{ detail.jobName }} (ID {{ detail.jobId }})</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail.repoName }}</el-descriptions-item>
        <el-descriptions-item label="触发">{{ detail.triggerType }}</el-descriptions-item>
        <el-descriptions-item label="Git">{{ detail.gitUrl }}</el-descriptions-item>
        <el-descriptions-item label="分支 / Commit">{{ detail.branch }} / {{ detail.commitHash }}</el-descriptions-item>
        <el-descriptions-item label="执行">
          <el-tag v-if="detail.execStatus === 1" type="success">成功</el-tag>
          <el-tag v-else-if="detail.execStatus === 2" type="danger">失败</el-tag>
          <el-tag v-else type="info">进行中</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="邮件">
          {{ emailLabel(detail.emailStatus) }}
        </el-descriptions-item>
        <el-descriptions-item label="时间">{{ detail.taskStartTime }} → {{ detail.taskEndTime || '—' }}</el-descriptions-item>
      </el-descriptions>
      <h4>Git 同步日志</h4>
      <el-input type="textarea" :rows="6" readonly :model-value="detail.cloneLog || ''" />
      <h4>执行命令</h4>
      <el-input type="textarea" :rows="4" readonly :model-value="detail.execCommand || ''" />
      <h4>执行输出</h4>
      <el-input type="textarea" :rows="14" readonly :model-value="detail.execResult || ''" />
    </template>
  </el-drawer>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { fetchActiveLogs, getActiveLog } from '@/api/activeScan'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const drawerVisible = ref(false)
const detail = ref(null)

function emailLabel(v) {
  if (v === 0) return '未发送'
  if (v === 1) return '已发送'
  if (v === 2) return '发送失败'
  return '—'
}

async function load() {
  loading.value = true
  try {
    const res = await fetchActiveLogs(page.value - 1, size.value)
    tableData.value = res.content || []
    total.value = res.totalElements || 0
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  detail.value = await getActiveLog(row.id)
  drawerVisible.value = true
}

onMounted(load)
</script>

<style scoped lang="scss">
.page-card {
  min-height: calc(100vh - 120px);
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
h4 {
  margin: 16px 0 8px;
}
</style>
