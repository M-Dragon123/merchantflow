<script setup lang="ts">
import { computed } from 'vue'

export interface TrendPoint {
  date: string
  amount: number
}
const props = defineProps<{ points: TrendPoint[] }>()

const W = 760
const H = 230
const PAD = { top: 18, right: 18, bottom: 34, left: 56 }

const values = computed(() => props.points.map((p) => p.amount))
const maxValue = computed(() => {
  const m = Math.max(...values.value, 0)
  return m === 0 ? 1 : m
})
const path = computed(() => {
  if (!props.points.length) return ''
  const step = (W - PAD.left - PAD.right) / Math.max(1, props.points.length - 1)
  return props.points
    .map((p, i) => {
      const x = PAD.left + i * step
      const y = PAD.top + (1 - p.amount / maxValue.value) * (H - PAD.top - PAD.bottom)
      return `${i === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
})
const areaPath = computed(() => {
  if (!path.value) return ''
  const step = (W - PAD.left - PAD.right) / Math.max(1, props.points.length - 1)
  const lastX = PAD.left + (props.points.length - 1) * step
  const base = H - PAD.bottom
  return `${path.value} L${lastX.toFixed(1)},${base} L${PAD.left},${base} Z`
})
const yTicks = computed(() => [0, 0.5, 1].map((r) => Math.round((maxValue.value * r) * 100) / 100))
const xLabelIndexes = computed(() => {
  const n = props.points.length
  if (n <= 1) return [0]
  const step = Math.max(1, Math.ceil(n / 6))
  const idxs: number[] = []
  for (let i = 0; i < n; i += step) idxs.push(i)
  if (idxs[idxs.length - 1] !== n - 1) idxs.push(n - 1)
  return idxs
})
function shortDate(iso: string) {
  return iso.slice(5).replace('-', '/')
}
</script>

<template>
  <div class="trend-chart">
    <svg :viewBox="`0 0 ${W} ${H}`" class="chart" role="img" aria-label="销售趋势折线图">
      <g v-for="(tick, i) in yTicks" :key="i">
        <line
          :x1="PAD.left"
          :x2="W - PAD.right"
          :y1="PAD.top + i * ((H - PAD.top - PAD.bottom) / 2)"
          :y2="PAD.top + i * ((H - PAD.top - PAD.bottom) / 2)"
          class="grid"
        />
        <text :x="PAD.left - 8" :y="PAD.top + i * ((H - PAD.top - PAD.bottom) / 2) + 4" class="y-label"
          >¥{{ tick }}</text
        >
      </g>
      <text :x="PAD.left - 8" :y="H - PAD.bottom + 4" class="y-label">¥0</text>
      <path v-if="areaPath" :d="areaPath" class="area" />
      <path v-if="path" :d="path" class="line" />
      <g v-for="i in xLabelIndexes" :key="i">
        <text
          :x="PAD.left + (i * (W - PAD.left - PAD.right)) / Math.max(1, points.length - 1)"
          :y="H - 10"
          class="x-label"
          text-anchor="middle"
          >{{ shortDate(points[i].date) }}</text
        >
      </g>
    </svg>
    <p v-if="!points.length" class="empty">近 N 天暂无已支付订单</p>
  </div>
</template>

<style scoped>
.trend-chart {
  position: relative;
}
.chart {
  display: block;
  width: 100%;
  height: auto;
}
.grid {
  stroke: #e8edf5;
  stroke-width: 1;
}
.line {
  fill: none;
  stroke: #2563eb;
  stroke-width: 2.5;
  stroke-linejoin: round;
  stroke-linecap: round;
}
.area {
  fill: rgba(37, 99, 235, 0.09);
}
.y-label {
  fill: #94a3b8;
  font-size: 11px;
  text-anchor: end;
}
.x-label {
  fill: #94a3b8;
  font-size: 11px;
}
.empty {
  position: absolute;
  inset: 40% 0 auto;
  text-align: center;
  color: #94a3b8;
  font-size: 0.85rem;
}
</style>
