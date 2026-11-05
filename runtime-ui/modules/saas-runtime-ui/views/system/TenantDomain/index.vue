<template>
  <j-page-container>
    <a-spin :spinning="loading" class="domain-page">
      <a-alert
        v-if="!capability?.enabled"
        type="warning"
        show-icon
        :message="$t('tenantDomain.disabled')"
      />

      <section v-else class="section section--card domain-section">
        <div class="section__title">{{ $t('tenantDomain.title') }}</div>
        <div class="domain-panel">
          <div class="domain-panel__intro">
            <div>
              <strong>{{ $t('tenantDomain.bindTitle') }}</strong>
              <p>{{ $t('tenantDomain.bindDescription') }}</p>
            </div>
            <span class="domain-panel__target">
              {{ $t('tenantDomain.cnameTarget') }}{{ $t('tenantDomain.cnameTargetSeparator') }}{{ capability?.cnameTarget || '-' }}
            </span>
          </div>

          <div class="domain-steps">
            <template v-for="(step, index) in steps" :key="step.title">
              <button
                class="domain-step"
                :class="stepClass(index)"
                type="button"
                @click="scrollToStep(index)"
              >
                <span class="domain-step__index">{{ `0${index + 1}` }}</span>
                <span class="domain-step__body">
                  <strong>{{ step.title }}</strong>
                  <small>{{ step.description }}</small>
                </span>
                <QuestionCircleOutlined class="domain-step__help" />
              </button>
              <span v-if="index < steps.length - 1" class="domain-steps__connector" />
            </template>
          </div>

          <div class="domain-form">
            <section id="tenant-domain-step-0" class="domain-form__section">
              <div class="domain-form__section-head">
                <span class="domain-form__step-number">01</span>
                <div>
                  <strong>{{ $t('tenantDomain.form.cnameTitle') }}</strong>
                  <span>{{ $t('tenantDomain.form.cnameDescription') }}</span>
                </div>
              </div>
              <div class="domain-field domain-field--compact">
                <div class="domain-field__control domain-field__control--full">
                  <a-input
                    v-model:value="form.domain"
                    :placeholder="$t('tenantDomain.domainPlaceholder')"
                    @input="resetWorkflow"
                  />
                  <button
                    class="domain-action"
                    type="button"
                    :disabled="!form.domain.trim() || cnameLoading"
                    @click="validateCname"
                  >
                    {{ $t('tenantDomain.validateCname') }}
                  </button>
                  <span v-if="cnameValidated" class="domain-field__status is-success">
                    <CheckCircleOutlined />{{ $t('tenantDomain.cnamePassed') }}
                  </span>
                </div>
              </div>
            </section>

            <section id="tenant-domain-step-1" class="domain-form__section">
              <div class="domain-form__section-head">
                <span class="domain-form__step-number">02</span>
                <div>
                  <strong>{{ $t('tenantDomain.form.certificateTitle') }}</strong>
                  <span>{{ $t('tenantDomain.form.certificateDescription') }}</span>
                </div>
              </div>
              <div class="domain-field domain-field--compact">
                <div class="domain-field__control domain-field__control--full">
                  <label class="domain-upload" :class="{ 'is-disabled': !cnameValidated || uploadLoading }">
                    <UploadOutlined />
                    <span>{{ $t('tenantDomain.privateKey') }}</span>
                    <small v-if="privateKeyFile" class="domain-upload__file">
                      {{ privateKeyFile.name }}
                    </small>
                    <input
                      type="file"
                      accept=".pem,.key,.p12,.pfx"
                      :disabled="!cnameValidated || uploadLoading"
                      @change="selectPrivateKey"
                    />
                  </label>
                  <label class="domain-upload" :class="{ 'is-disabled': !cnameValidated || uploadLoading }">
                    <UploadOutlined />
                    <span>{{ $t('tenantDomain.certificateChain') }}</span>
                    <small v-if="certificateFile" class="domain-upload__file">
                      {{ certificateFile.name }}
                    </small>
                    <input
                      type="file"
                      accept=".pem,.crt,.cer,.chain,.zip"
                      :disabled="!cnameValidated || uploadLoading"
                      @change="selectCertificate"
                    />
                  </label>
                  <span v-if="certificateUploaded" class="domain-field__status is-success">
                    <CheckCircleOutlined />{{ $t('tenantDomain.uploaded') }}
                  </span>
                </div>
              </div>
            </section>

            <section id="tenant-domain-step-2" class="domain-form__section">
              <div class="domain-form__section-head">
                <span class="domain-form__step-number">03</span>
                <div>
                  <strong>{{ $t('tenantDomain.form.effectivenessTitle') }}</strong>
                  <span>{{ $t('tenantDomain.form.effectivenessDescription') }}</span>
                </div>
              </div>
              <div class="domain-field domain-field--actions domain-field--compact">
                <div class="domain-field__control domain-field__control--full">
                  <button
                    class="domain-action"
                    type="button"
                    :disabled="!certificateUploaded || testLoading"
                    @click="testEffectiveness"
                  >
                    {{ $t('tenantDomain.test') }}
                  </button>
                  <button
                    class="domain-action domain-action--primary"
                    type="button"
                    :disabled="!effectivenessTested || saveLoading"
                    @click="saveConfiguration"
                  >
                    {{ $t('tenantDomain.save') }}
                  </button>
                  <span v-if="effectivenessTested" class="domain-field__status is-success">
                    <CheckCircleOutlined />{{ $t('tenantDomain.effectivenessPassed') }}
                  </span>
                </div>
              </div>
            </section>
          </div>

          <div v-if="domain" class="domain-status">
            <span>{{ $t('tenantDomain.status') }}：</span>
            <a-tag :color="stateColor">{{ stateText }}</a-tag>
            <a-button v-if="domain.state === 'failed'" type="link" @click="retry">
              {{ $t('tenantDomain.retry') }}
            </a-button>
            <a-popconfirm :title="$t('tenantDomain.deleteConfirm')" @confirm="remove">
              <a-button danger type="link">{{ $t('tenantDomain.delete') }}</a-button>
            </a-popconfirm>
          </div>
        </div>
      </section>
    </a-spin>
  </j-page-container>
</template>

<script setup lang="ts" name="TenantDomain">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  CheckCircleOutlined,
  QuestionCircleOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import {
  deleteTenantDomain,
  getTenantDomain,
  getTenantDomainCapability,
  getTenantDomainWorkflow,
  retryTenantDomain,
  saveTenantDomain,
  testTenantDomain,
  uploadTenantDomainCertificate,
  validateTenantDomainCname,
  type TenantDomainCertificateMode,
} from '../../../api/tenantDomain'

const { t: $t } = useI18n()

type State = 'pending_dns' | 'awaiting_upload' | 'issuing' | 'awaiting_certificate' | 'registering' | 'ready' | 'failed' | 'deleting'
interface Domain { domain: string; certificateMode: TenantDomainCertificateMode; state: State }
interface Workflow { domain: string; state: State; cnameValidated: boolean; certificateUploaded: boolean; effectivenessTested: boolean }
interface Capability { enabled: boolean; providerAvailable: boolean; cnameTarget?: string; supportedCertificateModes: TenantDomainCertificateMode[] }

const loading = ref(false)
const cnameLoading = ref(false)
const uploadLoading = ref(false)
const testLoading = ref(false)
const saveLoading = ref(false)
const domain = ref<Domain>()
const capability = ref<Capability>()
const cnameValidated = ref(false)
const certificateUploaded = ref(false)
const effectivenessTested = ref(false)
const validatedDomain = ref('')
const privateKeyFile = ref<File>()
const certificateFile = ref<File>()
const form = reactive({ domain: '', certificateMode: 'uploaded' as TenantDomainCertificateMode })

const unwrap = <T,>(response: { result?: T }) => response?.result
const steps = computed(() => [
  { title: $t('tenantDomain.step.cname'), description: $t('tenantDomain.cnameDescription') },
  { title: $t('tenantDomain.step.certificate'), description: $t('tenantDomain.certificateDescription') },
  { title: $t('tenantDomain.step.effectiveness'), description: $t('tenantDomain.effectivenessDescription') },
])
const currentStep = computed(() => {
  if (!cnameValidated.value) return 0
  if (!certificateUploaded.value) return 1
  return 2
})
const stateText = computed(() => ({
  pending_dns: $t('tenantDomain.state.pendingDns'),
  awaiting_upload: $t('tenantDomain.state.awaitingUpload'),
  issuing: $t('tenantDomain.state.issuing'),
  awaiting_certificate: $t('tenantDomain.state.awaitingCertificate'),
  registering: $t('tenantDomain.state.registering'),
  ready: $t('tenantDomain.state.ready'),
  failed: $t('tenantDomain.state.failed'),
  deleting: $t('tenantDomain.state.deleting'),
}[domain.value?.state || 'pending_dns'] || domain.value?.state || ''))
const stateColor = computed(() => domain.value?.state === 'ready' ? 'success' : domain.value?.state === 'failed' ? 'error' : 'processing')

const stepClass = (index: number) => ({
  'is-current': index === currentStep.value,
  'is-done': (index === 0 && cnameValidated.value) || (index === 1 && certificateUploaded.value) || (index === 2 && effectivenessTested.value),
  'is-pending': index > currentStep.value,
})

const scrollToStep = (index: number) => {
  document.getElementById(`tenant-domain-step-${index}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

const resetWorkflow = () => {
  cnameValidated.value = false
  certificateUploaded.value = false
  effectivenessTested.value = false
  validatedDomain.value = ''
  privateKeyFile.value = undefined
  certificateFile.value = undefined
}

const refresh = async () => {
  const [domainResponse, workflowResponse] = await Promise.all([getTenantDomain(), getTenantDomainWorkflow()])
  domain.value = unwrap<Domain>(domainResponse)
  const workflow = unwrap<Workflow>(workflowResponse)
  if (domain.value) form.domain = domain.value.domain
  if (workflow) {
    cnameValidated.value = workflow.cnameValidated
    certificateUploaded.value = workflow.certificateUploaded
    effectivenessTested.value = workflow.effectivenessTested
    validatedDomain.value = workflow.cnameValidated ? workflow.domain : ''
  }
}

// CNAME 校验成功后才允许上传证书，避免证书材料进入未确认的域名流程。
const validateCname = async () => {
  const domainName = form.domain.trim()
  if (!domainName) return
  cnameLoading.value = true
  try {
    const valid = Boolean(unwrap<boolean>(await validateTenantDomainCname(domainName)))
    cnameValidated.value = valid
    validatedDomain.value = valid ? domainName : ''
    if (valid) {
      await saveTenantDomain({ domain: domainName, certificateMode: form.certificateMode })
      await refresh()
      message.success($t('tenantDomain.cnamePassed'))
    } else {
      message.warning($t('tenantDomain.cnameFailed'))
    }
  } finally {
    cnameLoading.value = false
  }
}

const tryUploadCertificate = async () => {
  if (validatedDomain.value !== form.domain.trim() || !privateKeyFile.value || !certificateFile.value || uploadLoading.value) return
  uploadLoading.value = true
  try {
    await uploadTenantDomainCertificate(certificateFile.value, privateKeyFile.value)
    certificateUploaded.value = true
    await refresh()
    message.success($t('tenantDomain.uploaded'))
  } finally {
    uploadLoading.value = false
  }
}

const selectPrivateKey = (event: Event) => {
  privateKeyFile.value = (event.target as HTMLInputElement).files?.[0]
  void tryUploadCertificate()
}

const selectCertificate = (event: Event) => {
  certificateFile.value = (event.target as HTMLInputElement).files?.[0]
  void tryUploadCertificate()
}

const testEffectiveness = async () => {
  testLoading.value = true
  try {
    effectivenessTested.value = Boolean(unwrap<boolean>(await testTenantDomain()))
    await refresh()
    effectivenessTested.value
      ? message.success($t('tenantDomain.effectivenessPassed'))
      : message.warning($t('tenantDomain.effectivenessFailed'))
  } finally {
    testLoading.value = false
  }
}

const saveConfiguration = async () => {
  if (!effectivenessTested.value) return
  saveLoading.value = true
  try {
    await saveTenantDomain({ domain: form.domain.trim(), certificateMode: 'uploaded' })
    await refresh()
    message.success($t('tenantDomain.saved'))
  } finally {
    saveLoading.value = false
  }
}

const retry = async () => { await retryTenantDomain(); await refresh() }
const remove = async () => {
  await deleteTenantDomain()
  domain.value = undefined
  resetWorkflow()
  form.domain = ''
  message.success($t('tenantDomain.deleted'))
}

onMounted(async () => {
  loading.value = true
  try {
    capability.value = unwrap<Capability>(await getTenantDomainCapability())
    if (capability.value?.enabled) await refresh()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="less">
.domain-page { display: block; }
.section--card {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 20px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgb(17 24 39 / 4%);
}
.section__title { color: #1a1a1a; font-size: 18px; line-height: 18px; }
.domain-panel { width: 100%; box-sizing: border-box; }
.domain-panel__intro { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
.domain-panel__intro strong { color: #1a1a1a; font-size: 14px; }
.domain-panel__intro p { margin: 6px 0 0; color: rgb(0 0 0 / 45%); font-size: 13px; }
.domain-panel__target { flex-shrink: 0; padding: 5px 9px; border-radius: 6px; background: #f0f7ff; color: #1677ff; font-size: 12px; }
.domain-steps { display: grid; grid-template-columns: minmax(0, 1fr) 28px minmax(0, 1fr) 28px minmax(0, 1fr); align-items: center; margin-top: 20px; }
.domain-step { min-width: 0; display: flex; align-items: flex-start; gap: 10px; padding: 12px; border: 0; border-radius: 9px; background: #f7f8fa; color: #111418; font-family: inherit; text-align: left; cursor: pointer; transition: background .15s, box-shadow .15s; }
.domain-step:hover, .domain-step.is-current { background: #f0f7ff; box-shadow: 0 0 0 2px rgb(22 119 255 / 8%); }
.domain-step.is-done .domain-step__index { background: #e8f8f0; color: #16a34a; }
.domain-step.is-pending { color: #8895ab; }
.domain-step.is-pending .domain-step__index { background: #f2f4f7; color: #9aa1ab; }
.domain-step__index { width: 28px; height: 28px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; border-radius: 50%; background: #e8f1ff; color: #1677ff; font-size: 11px; font-weight: 600; }
.domain-step__body { min-width: 0; display: flex; flex: 1; flex-direction: column; gap: 4px; }
.domain-step__body strong { overflow: hidden; color: #1c2842; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.domain-step__body small { display: -webkit-box; overflow: hidden; color: #9aa1ab; font-size: 11px; line-height: 1.5; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.domain-step__help { flex-shrink: 0; margin-top: 2px; color: #91a3b8; font-size: 14px; }
.domain-steps__connector { height: 1px; margin: 0 4px; background: #dbe4f1; }
.domain-form { display: flex; flex-direction: column; gap: 12px; margin-top: 20px; padding-top: 18px; border-top: 1px solid #eef1f5; }
.domain-form__section { padding: 14px 16px; border-radius: 8px; background: #f7f8fa; }
.domain-form__section-head { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 16px; }
.domain-form__section-head > div { display: flex; flex-direction: column; gap: 4px; }
.domain-form__section-head strong { color: #1c2842; font-size: 13px; }
.domain-form__section-head span:not(.domain-form__step-number) { color: #9aa1ab; font-size: 11px; }
.domain-form__step-number { color: #1677ff; font-size: 12px; font-weight: 600; line-height: 18px; }
.domain-field { display: grid; grid-template-columns: 112px minmax(260px, 1fr) auto; align-items: center; gap: 10px 14px; }
.domain-field__control { min-width: 0; display: flex; align-items: center; gap: 8px; }
.domain-field__control--full { grid-column: 1 / -1; flex-wrap: wrap; margin-left: 30px; }
.domain-field__control--full :deep(.ant-input) { min-width: 0; flex: 1; }
.domain-field--actions { align-items: start; }
.domain-field__status { display: inline-flex; align-items: center; gap: 4px; white-space: nowrap; font-size: 11px; }
.domain-field__status.is-success { color: #16a34a; }
.domain-action { height: 32px; flex-shrink: 0; padding: 0 12px; border: 1px solid #dce4f0; border-radius: 6px; background: #fff; color: #1677ff; font-family: inherit; font-size: 12px; cursor: pointer; transition: color .15s, border-color .15s, background .15s; }
.domain-action:hover:not(:disabled) { border-color: #1677ff; background: #f0f7ff; }
.domain-action:disabled { cursor: not-allowed; opacity: .5; }
.domain-action--primary { border-color: #1677ff; background: #1677ff; color: #fff; }
.domain-action--primary:hover:not(:disabled) { border-color: #0958d9; background: #0958d9; color: #fff; }
.domain-upload { height: 32px; display: inline-flex; align-items: center; gap: 6px; padding: 0 12px; border: 1px dashed #91caff; border-radius: 6px; background: #f7fbff; color: #1677ff; font-size: 12px; cursor: pointer; }
.domain-upload:hover:not(.is-disabled) { border-color: #1677ff; background: #f0f7ff; }
.domain-upload.is-disabled { cursor: not-allowed; opacity: .5; }
.domain-upload input { display: none; }
.domain-upload__file { max-width: 300px; overflow: hidden; color: #3a3f47; text-overflow: ellipsis; white-space: nowrap; }
.domain-status { display: flex; align-items: center; gap: 8px; margin-top: 20px; padding-top: 16px; border-top: 1px solid #eef1f5; color: #3a3f47; font-size: 12px; }
@media (max-width: 900px) {
  .domain-panel__intro { flex-direction: column; gap: 10px; }
  .domain-steps { grid-template-columns: 1fr; gap: 8px; }
  .domain-steps__connector { width: 1px; height: 12px; margin: 0 0 0 26px; }
  .domain-field { grid-template-columns: 100px minmax(0, 1fr); }
  .domain-field__control--full { grid-column: 1 / -1; }
}
@media (max-width: 640px) {
  .section--card { padding: 14px; }
  .domain-field { grid-template-columns: 1fr; }
  .domain-field__control--full { grid-column: 1; margin-left: 0; }
  .domain-field__control--full :deep(.ant-input) { flex-basis: 100%; }
  .domain-upload { max-width: 100%; }
}
</style>
