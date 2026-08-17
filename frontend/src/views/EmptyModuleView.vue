<template>
  <el-empty description="该模块将在下一阶段开放"
    ><template #image><div class="placeholder-mark">MF</div></template
    ><el-button type="primary" @click="$router.push('/')">返回工作台</el-button></el-empty
  >
</template>
<style scoped>
.placeholder-mark {
  display: grid;
  width: 92px;
  height: 92px;
  place-items: center;
  color: #1d4ed8;
  border: 1px dashed #93c5fd;
  border-radius: 50%;
  font-weight: 700;
}
</style>
