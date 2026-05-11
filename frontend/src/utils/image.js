// 默认占位图 (Base64 SVG) - 灰色背景，中间显示"暂无图片"
export const DEFAULT_IMAGE = `data:image/svg+xml;charset=UTF-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22300%22%20height%3D%22200%22%20viewBox%3D%220%200%20300%20200%22%3E%3Crect%20fill%3D%22%23f0f0f0%22%20width%3D%22100%25%22%20height%3D%22100%25%22%2F%3E%3Ctext%20x%3D%2250%25%22%20y%3D%2250%25%22%20font-family%3D%22%E5%BE%AE%E8%BD%AF%E9%9B%85%E9%BB%91%2CArial%22%20font-size%3D%2224%22%20fill%3D%22%23ccc%22%20dominant-baseline%3D%22middle%22%20text-anchor%3D%22middle%22%3E%E6%9A%82%E6%97%A0%E5%9B%BE%E7%89%87%3C%2Ftext%3E%3C%2Fsvg%3E`

// 图片加载失败处理函数
export const handleImageError = (e) => {
    // 防止无限循环（如果默认图片也加载失败）
    if (e.target.src !== DEFAULT_IMAGE) {
        e.target.src = DEFAULT_IMAGE
        // 添加此时的样式，防止原来的alt文字重叠
        e.target.style.objectFit = 'cover'
    }
}
