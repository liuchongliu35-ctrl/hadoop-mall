<template>
  <div class="page notfound">
    <h2>页面走丢了</h2>
    <p class="muted">请求的页面不存在，返回首页继续逛逛吧。</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.notfound {
  text-align: center;
  padding: 60px 0;
}
</style>
