<template>
  <a-modal
      :open="true"
      :title="$t('SaasManager.generated.ffdc4bd6f515')"
      :width="600"
      :footer="null"
      :closable="true"
      @cancel="handleClose"
  >
    <div class="success-container">
      <!-- 插画区域 -->
      <div class="illustration">
        <div class="success-illustration">
          <!-- 这里可以放置自定义插画，现在用简化的图标组合 -->
          <div class="lock-icon">
            <AIcon type="LockOutlined"/>
          </div>
          <div class="hand-icon">
            <AIcon type="SafetyOutlined"/>
          </div>
        </div>
      </div>

      <!-- 成功文本 -->
      <div class="success-title"> {{ $t('SaasManager.generated.afc93dd0d035') }} </div>

      <!-- Token 展示区域 -->
        <div class="token-section">
            <div class="token-display">
                <span>key: </span>
                <span>{{key}}</span>
            </div>
        </div>
      <div class="token-section">
        <div class="token-display">
          <a-input
              :value="token"
              readonly
              class="token-input"
          />
          <a-button
              type="primary"
              class="copy-button"
              @click="copyToken"
          >
            <AIcon type="CopyOutlined"/>
          </a-button>
        </div>
      </div>

      <!-- 说明文本 -->
      <div class="warning-section">
        <AIcon type="ExclamationCircleOutlined" class="warning-icon"/>
        <span class="warning-text"> {{ $t('SaasManager.generated.6e094dd335aa') }} </span>
      </div>

      <!-- 关闭按钮 -->
      <div class="footer-actions">
        <a-button type="default" @click="handleClose"> {{ $t('Service.index.close') }} </a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import globalI18n from '@jetlinks-web-core/locales'


import {onlyMessage} from "@jetlinks-web/utils";

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  token: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['close'])

const key = import.meta.env.VITE_PERSONAL_TOKEN_KEY

async function copy(text) {
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(text);
  } else {
    const textArea = document.createElement("textarea");
    textArea.value = text;
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    document.execCommand("copy");
    document.body.removeChild(textArea);
  }
  onlyMessage(globalI18n.global.t('SaasManager.generated.96db0cae56e8'))
}


const copyToken = () => {
  copy(props.token)
}

const handleClose = () => {
  emit('close')
}
</script>

<style lang="less" scoped>
.success-container {
  text-align: center;
  padding: 2.5rem 1.5rem 1.5rem;

  .illustration {
    margin-bottom: var(--space-6);
    height: 7.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;

    .success-illustration {
      position: relative;
      width: 7.5rem;
      height: 7.5rem;

      .lock-icon {
        position: absolute;
        top: 1.25rem;
        left: 1.875rem;
        font-size: var(--fs-48);
        color: var(--jet-theme-primary);
        z-index: 2;
      }

      .hand-icon {
        position: absolute;
        bottom: 1.25rem;
        right: 1.25rem;
        font-size: var(--fs-36);
        color: var(--jet-theme-warning);
        z-index: 1;
      }

      // 添加一些装饰圆点
      &::before {
        content: '';
        position: absolute;
        top: 0.625rem;
        right: 0.625rem;
        width: 0.375rem;
        height: 0.375rem;
        border-radius: 50%;
        background-color: var(--jet-theme-border-secondary);
      }

      &::after {
        content: '';
        position: absolute;
        bottom: 2.5rem;
        left: 0.625rem;
        width: 0.25rem;
        height: 0.25rem;
        border-radius: 50%;
        background-color: var(--jet-theme-primary);
      }
    }
  }

  .success-title {
    font-size: var(--fs-18);
    font-weight: 500;
    color: var(--jet-theme-text-title);
    margin-bottom: var(--space-6);
  }

  .token-section {
    margin-bottom: var(--space-4);

    .token-display {
      display: flex;
      gap: var(--space-2);
      align-items: center;

      .token-input {
        flex: 1; :deep(.ant-input) {
          background-color: var(--color-jet-gray-50);
          border: 1px solid var(--jet-theme-border-secondary);
          padding: 0.5rem 0.75rem;
          font-size: var(--fs-13);
          color: var(--jet-theme-text);
        }
      }

      .copy-button {
        flex-shrink: 0;
        padding: 0 1rem;
        display: flex;
        align-items: center;
        gap: var(--space-1);
        background: var(--jet-theme-primary);
        border-color: var(--jet-theme-primary);

        &:hover {
          background: var(--jet-theme-primary-hover);
          border-color: var(--jet-theme-primary-hover);
        }
      }
    }
  }

  .warning-section {
    display: flex;
    align-items: flex-start;
    gap: var(--space-2);
    margin-bottom: var(--space-8);
    text-align: left;

    .warning-icon {
      color: var(--jet-theme-warning);
      font-size: var(--fs-16);
      margin-top: 0.125rem;
      flex-shrink: 0;
    }

    .warning-text {
      color: var(--jet-theme-text-secondary);
      font-size: var(--fs-13);
      line-height: 1.5;
    }
  }

  .footer-actions {
    text-align: right;

    .ant-btn {
      min-width: 4.5rem;
      border-radius: var(--r-1);
    }
  }
}

// 自定义modal样式
:deep(.ant-modal) {
  .ant-modal-header {
    border-bottom: 1px solid var(--line-strong);
    padding: 1rem 1.5rem;

    .ant-modal-title {
      font-size: var(--fs-16);
      font-weight: 500;
    }
  }

  .ant-modal-body {
    padding: 0;
  }

  .ant-modal-close {
    top: 1rem;
    right: 1rem;
  }
}</style>
