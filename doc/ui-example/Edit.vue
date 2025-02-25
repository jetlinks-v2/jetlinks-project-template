<script setup >
import {
  addUser_api,
  updateUser_api,
} from '@/api/system/user';
import { onlyMessage } from '@/utils/comm';
import { useI18n } from 'vue-i18n';
import { useRequest } from "@jetlinks-web/hooks";

const { t: $t } = useI18n();

const emits = defineEmits(['confirm', 'update:visible']);
const props = defineProps({
  type: {
    type: String,
    default: 'add'
  },
  data: {
    type: Object,
    default: () => ({})
  }
});

const { loading, run } = useRequest(props.type === 'add' ? addUser_api: updateUser_api)
const formRef = ref();
const formData = reactive({
  name: null,
  telephone: null,
})

const dialogTitle = computed(() => {
  return props.type === 'add' ? $t('components.EditUserDialog.939453-23') : $t('components.EditUserDialog.939453-24')
});

const confirm = async () => {
  const result = await formData.value?.validateFields()
  if (result) {
    const { positions, ...extraFormData} = formData
    const params = {
      id: formData.id,
      user: extraFormData,
      orgIdList: formData.orgIdList,
      roleIdList: formData.roleIdList,
      positions: positions,
    };
    run(params)
  }
}
</script>

<template>
  <a-modal
    visible
    width="675px"
    :title="dialogTitle"
    :maskClosable="false"
    :confirmLoading="loading"
    :cancelText="$t('components.EditUserDialog.939453-0')"
    :okText="$t('components.EditUserDialog.939453-1')"
    @ok="confirm"
    @cancel="emits('update:visible', false)"
  >
    <a-form ref="formRef" :model="formData" layout="vertical">
      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item
            name="name"
            :label="$t('components.EditUserDialog.939453-3')"
            :rules="[
                            { required: true, message: $t('components.EditUserDialog.939453-4') },
                            {
                                max: 64,
                                message: $t('components.EditUserDialog.939453-5'),
                            },
                        ]"
          >
            <a-input
              v-model:value="formData.name"
              :placeholder="$t('components.EditUserDialog.939453-4')"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            name="telephone"
            :label="$t('components.EditUserDialog.939453-6')"
            :rules="[
                            {
                                pattern: /^1[3456789]\d{9}$/,
                                message: $t('components.EditUserDialog.939453-7'),
                            },
                        ]"
          >
            <a-input
              v-model:value="formData.telephone"
              :placeholder="$t('components.EditUserDialog.939453-8')"
              :maxlength="64"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="24">
        <a-col :span="24">
          <a-form-item
            name="password"
            :label="$t('components.EditUserDialog.939453-19')"
            :rules="[
                            { required: true, message: $t('components.EditUserDialog.939453-20') },
                            {
                                validator: formData.checkPassword,
                                trigger: 'change',
                            },
                        ]"
          >
            <a-input-password
              v-model:value="formData.password"
              :placeholder="$t('components.EditUserDialog.939453-20')"
            />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<style scoped lang="less">

</style>
