<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <span>扫描任务日志</span>
        <div class="header-actions">
          <el-select
            v-model="filterProjectId"
            clearable
            filterable
            placeholder="项目名称"
            style="width: 220px"
            @change="onFilterChange"
          >
            <el-option
              v-for="o in projectOptions"
              :key="o.id"
              :label="o.name"
              :value="o.id"
            />
          </el-select>
          <el-button @click="load">刷新</el-button>
        </div>
      </div>
    </template>
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="projectName" label="项目" min-width="120" />
      <el-table-column prop="branch" label="分支" width="120" show-overflow-tooltip />
      <el-table-column prop="commitHash" label="Commit" min-width="160" show-overflow-tooltip />
      <el-table-column prop="commitUser" label="提交人" width="120" show-overflow-tooltip />
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
          <span v-else-if="row.emailStatus === 2" style="color: #f56c6c">发送失败</span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="taskStartTime" label="开始时间" width="170">
        <template #default="{ row }">{{ formatBackendDateTime(row.taskStartTime) }}</template>
      </el-table-column>
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

  <el-drawer v-model="drawerVisible" title="执行详情" size="60%">
    <template v-if="detail">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="项目">{{ detail.projectName }} (GitLab {{ detail.gitlabProjectId }})</el-descriptions-item>
        <el-descriptions-item label="分支">{{ detail.branch }}</el-descriptions-item>
        <el-descriptions-item label="Commit">{{ detail.commitHash }}</el-descriptions-item>
        <el-descriptions-item label="提交人">{{ detail.commitUser }}</el-descriptions-item>
        <el-descriptions-item label="执行状态">
          <el-tag v-if="detail.execStatus === 1" type="success">成功</el-tag>
          <el-tag v-else-if="detail.execStatus === 2" type="danger">失败</el-tag>
          <el-tag v-else type="info">进行中</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="邮件状态">
          {{ emailLabel(detail.emailStatus) }}
        </el-descriptions-item>
        <el-descriptions-item label="开始">{{ formatBackendDateTime(detail.taskStartTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束">{{ formatBackendDateTime(detail.taskEndTime) }}</el-descriptions-item>
      </el-descriptions>
      <h4>执行命令</h4>
      <el-input type="textarea" :rows="4" readonly :model-value="detail.execCommand || ''" />
      <h4>执行结果</h4>
      <MarkdownOutputPanel :text="detail.execResult || ''" :rows="16" />
    </template>
  </el-drawer>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { fetchScanLogs, getScanLog } from '@/api/scanLog'
import { fetchWebhookProjectOptions } from '@/api/projectOptions'
import MarkdownOutputPanel from '@/components/MarkdownOutputPanel.vue'
import { formatBackendDateTime } from '@/utils/formatTime'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filterProjectId = ref(null)
const projectOptions = ref([])

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
    const res = await fetchScanLogs(page.value - 1, size.value, filterProjectId.value ?? undefined)
    tableData.value = res.content || []
    total.value = res.totalElements || 0
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  page.value = 1
  load()
}

onMounted(async () => {
  try {
    projectOptions.value = await fetchWebhookProjectOptions()
  } catch {
    projectOptions.value = []
  }
  await load()
})

async function openDetail(row) {
  detail.value = await getScanLog(row.id)
  drawerVisible.value = true
}

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
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
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
