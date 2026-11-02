<template>
  <j-page-container>
    <FullPage>
      <div class="tag-container">
        <div class="left">
          <CategoryList @change="onCategoryChange" />
        </div>
        <div class="right">
          <TagTree :categoryId="categoryId" />
        </div>
      </div>
    </FullPage>
  </j-page-container>
</template>

<script setup lang="ts" name="Tag">
import CategoryList from './components/CategoryList.vue'
import TagTree from './components/TagTree.vue'

const categoryId = ref<string>('')

const onCategoryChange = (id: string) => {
  categoryId.value = id
}
</script>

<style lang="less" scoped>
.tag-container {
  display: flex;
  background-color: #fff;
  padding: 24px;
  height: 100%;
  position: relative;

  .left {
    border-right: 1px solid #f0f0f0;
    padding-right: 24px;
    height: 100%;
    width: 320px;
  }

  .right {
    flex: 1 1 0;
    min-width: 0;
    height: 100%;
  }
}
</style>
