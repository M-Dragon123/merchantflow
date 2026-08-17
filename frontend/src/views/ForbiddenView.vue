<template>
  <main class="error-page">
    <p>403</p>
    <h1>你没有访问此页面的权限</h1>
    <el-button type="primary" @click="$router.push('/')">返回工作台</el-button>
  </main>
</template>
<style scoped>
.error-page {
  min-height: 100vh;
  display: grid;
  place-content: center;
  text-align: center;
}
.error-page p {
  color: #f97316;
  font-size: 5rem;
  font-weight: 700;
  margin: 0;
}
.error-page h1 {
  font-size: 1.4rem;
}
</style>
