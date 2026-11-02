<!-- 产品模板基本信息 -->
<template>
    <a-descriptions bordered>
        <template #title>
            <div style="display: flex">
                <h3>{{ $t('template.detail.basic') || '基本信息' }}</h3>
                <j-permission-button
                    type="link"
                    @click="editConfig"
                    hasPermission="device/Template:update"
                >
                    <template #icon><AIcon type="EditOutlined" /></template>
                </j-permission-button>
            </div>
        </template>

        <a-descriptions-item :label="$t('template.detail.info.id') || 'ID'">
            <j-ellipsis>{{ templateStore.current.id }}</j-ellipsis>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('template.detail.info.name') || '模板名称'">
            <j-ellipsis>{{ templateStore.current.name }}</j-ellipsis>
        </a-descriptions-item>
        <!-- <a-descriptions-item :label="$t('template.detail.info.deviceType') || '设备类型'">{{
            templateStore.current.deviceType?.text
        }}</a-descriptions-item> -->
        <a-descriptions-item :label="$t('template.detail.info.access') || '接入方式'">
            <j-permission-button
                type="link"
                style="width:100%;padding:0"
                @click="changeTables"
                hasPermission="device/Template:update"
            >
                <div style="white-space: normal">
                    <j-ellipsis>{{
                        templateStore.current.accessName
                            ? templateStore.current.accessName
                            : ($t('template.list.notConfigured') || '未配置')
                    }}</j-ellipsis>
                </div>
            </j-permission-button>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('template.detail.info.createTime') || '创建时间'">{{
            dayjs(templateStore.current.createTime).format('YYYY-MM-DD HH:mm:ss')
        }}</a-descriptions-item>
        <a-descriptions-item :label="$t('template.detail.info.modifyTime') || '修改时间'">{{
            dayjs(templateStore.current.modifyTime).format('YYYY-MM-DD HH:mm:ss')
        }}</a-descriptions-item>

        <a-descriptions-item :label="$t('template.detail.info.description') || '说明'" :span="3">
            {{ templateStore.current.describe }}
        </a-descriptions-item>
    </a-descriptions>

    <Save ref="saveRef" :isAdd="2" :title="$t('template.form.title.edit') || '编辑模板'" @success="refresh" />
</template>

<script lang="ts" setup name="TemplateBasicInfo">
import { useTemplateStore } from '../../../../../store/template';
import Save from '../../Save/index.vue';
import dayjs from 'dayjs';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';

const { t: $t } = useI18n();
const templateStore = useTemplateStore();
const route = useRoute();
const saveRef = ref();

/**
 * 编辑配置信息
 */
const editConfig = () => {
    saveRef.value.show(templateStore.current);
};

/**
 * 切换到设备接入标签页
 */
const changeTables = () => {
    templateStore.tabActiveKey = 'Device';
};

/**
 * 修改成功刷新
 */
const refresh = () => {
    templateStore.refresh(route.params.id as string);
};
</script>
