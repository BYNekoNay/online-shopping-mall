/**
 * ECharts 按需引入配置（F-1）
 * 仅注册本项目用到的图表与组件，显著减小打包体积。
 */
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart, FunnelChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent, DataZoomComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  FunnelChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DataZoomComponent,
  CanvasRenderer
])

export default echarts
