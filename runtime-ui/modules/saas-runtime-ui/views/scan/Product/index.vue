<template>
  <main class="runtime-scan" :class="{ 'is-success': !!createdDevice }">
    <!-- 顶部栏 -->
    <header class="runtime-scan__topbar">
      <div class="runtime-scan__brand">
        <span class="runtime-scan__logo">
          <AIcon type="ApiOutlined" />
        </span>
        <div class="runtime-scan__brand-meta">
          <span>{{ projectName }}</span>
          <em>{{ headerSubtitle }}</em>
        </div>
      </div>
      <ol v-if="!createdDevice && !initError" class="runtime-scan__steps">
        <li class="is-done"><span><AIcon type="CheckOutlined" /></span></li>
        <li class="is-done"><span><AIcon type="CheckOutlined" /></span></li>
        <li :class="{ 'is-active': prepared && !preparing }"><span>3</span></li>
      </ol>
    </header>

    <!-- 加载初始化 -->
    <section v-if="preparing" class="runtime-scan__panel runtime-scan__loading">
      <div class="runtime-scan__loading-visual" aria-hidden="true">
        <span class="runtime-scan__ring" />
        <span class="runtime-scan__ring runtime-scan__ring--delay" />
        <span class="runtime-scan__loading-icon">
          <AIcon type="ApiOutlined" />
        </span>
      </div>
      <h1>初始化接入环境</h1>
      <p>正在为 {{ projectName }} 准备接入通道，请稍候</p>
    </section>

    <!-- 成功 -->
    <section v-else-if="createdDevice" class="runtime-scan__panel runtime-scan__success" role="status">
      <div class="runtime-scan__success-check" aria-hidden="true">
        <AIcon type="CheckOutlined" />
      </div>
      <h1>接入完成</h1>
      <p><strong>{{ createdDevice.name }}</strong> 已成功接入项目</p>
      <div class="runtime-scan__success-meta">
        <div>
          <span>项目</span>
          <strong>{{ projectName }}</strong>
        </div>
        <div v-if="assetModelText !== '--'">
          <span>网关</span>
          <strong>{{ assetModelText }}</strong>
        </div>
      </div>
      <div class="runtime-scan__desktop-guide">
        <AIcon type="DesktopOutlined" aria-hidden="true" />
        <div>
          <h2>{{ t('mobile.scan.success.desktopTitle') }}</h2>
          <p>{{ t('mobile.scan.success.desktopDescription', { project: projectName }) }}</p>
        </div>
      </div>
      <p class="runtime-scan__close-hint">{{ t('mobile.scan.success.closeHint') }}</p>
    </section>

    <!-- 错误 -->
    <section v-else-if="initError" class="runtime-scan__panel runtime-scan__error">
      <div class="runtime-scan__error-icon">
        <AIcon type="ExclamationOutlined" />
      </div>
      <h1>暂时无法继续接入</h1>
      <p>{{ initError }}</p>
    </section>

    <!-- 主流程 -->
    <section v-else class="runtime-scan__panel runtime-scan__ready">
      <!-- 网关摘要卡 -->
      <div v-if="hasAssetSummary" class="runtime-scan__device">
        <div class="runtime-scan__device-icon">
          <AIcon type="ApiOutlined" />
        </div>
        <div class="runtime-scan__device-meta">
          <span class="runtime-scan__device-tag">
            <AIcon type="CheckCircleFilled" />
            已识别
          </span>
          <strong>{{ assetModelText }}</strong>
          <span class="runtime-scan__device-sn" v-if="assetSerialText !== '--'">
            SN · {{ assetSerialText }}
          </span>
        </div>
      </div>

      <!-- 表单 -->
      <a-form
        ref="formRef"
        layout="vertical"
        :model="deviceForm"
        :rules="rules"
        class="runtime-scan__form"
      >
        <a-form-item label="网关名称" name="name">
          <a-input
            v-model:value="deviceForm.name"
            :maxlength="64"
            size="large"
            placeholder="给这台网关起个名字"
            allow-clear
            @focus="handleInputFocus"
          >
            <template #prefix>
              <AIcon type="EditOutlined" style="color: #94a3b8;" />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item label="备注" name="description">
          <a-textarea
            v-model:value="deviceForm.description"
            :maxlength="200"
            :auto-size="{ minRows: 3, maxRows: 5 }"
            placeholder="可填写网关位置、用途等"
            show-count
            @focus="handleInputFocus"
          />
        </a-form-item>

        <a-alert
          v-if="submitError"
          type="error"
          show-icon
          :message="submitError"
          class="runtime-scan__alert"
        />
      </a-form>
    </section>

    <!-- 固定底部 CTA -->
    <footer
      v-if="!preparing && !createdDevice"
      class="runtime-scan__footer"
    >
      <a-button
        v-if="initError"
        type="primary"
        block
        size="large"
        class="runtime-scan__cta"
        :loading="preparing"
        @click="initAccess"
      >
        <AIcon type="ReloadOutlined" />
        <span>重新检测</span>
      </a-button>
      <a-button
        v-else
        type="primary"
        block
        size="large"
        class="runtime-scan__cta"
        :loading="submitting"
        :disabled="!canSubmit"
        @click="handleConfirm"
      >
        <span>{{ submitting ? '正在接入…' : '确认接入' }}</span>
      </a-button>
    </footer>
  </main>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useScanProductAccess } from './hooks/useScanProductAccess'

const { t } = useI18n()

const {
  assetModelText,
  assetSerialText,
  canSubmit,
  createdDevice,
  deviceForm,
  formRef,
  handleConfirm,
  handleInputFocus,
  hasAssetSummary,
  headerSubtitle,
  initAccess,
  initError,
  prepared,
  preparing,
  projectName,
  rules,
  submitError,
  submitting,
} = useScanProductAccess()
</script>

<style scoped lang="less">
@import './index.less';
</style>
