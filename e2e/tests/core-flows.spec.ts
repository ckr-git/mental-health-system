import { test, expect } from '@playwright/test'

const BASE_URL = 'http://localhost:5173'

async function login(page, username, password) {
  await page.goto(`${BASE_URL}/login`)
  await page.getByRole('textbox', { name: '请输入用户名' }).fill(username)
  await page.getByRole('textbox', { name: '请输入密码' }).fill(password)
  await page.getByRole('button', { name: '登录' }).click()
}

test('patient login redirects to dashboard', async ({ page }) => {
  await login(page, 'patient001', '123456')
  await page.waitForURL(/\/patient/)
  await expect(page.getByText('晚上好').or(page.getByText('早上好')).or(page.getByText('下午好'))).toBeVisible()
})

test('patient mood diary shows latest created entry', async ({ page }) => {
  await login(page, 'patient001', '123456')
  await page.goto(`${BASE_URL}/patient/mood-diary`)
  await expect(page.getByRole('heading', { name: 'autotest 新增情绪日记' })).toBeVisible()
})

test('doctor login opens patient management surfaces', async ({ page }) => {
  await login(page, 'doctor001', '123456')
  await page.waitForURL(/\/doctor/)
  await page.goto(`${BASE_URL}/doctor/patients`)
  await expect(page.getByText('患者姓名')).toBeVisible()
  await expect(page.getByText('E2E测试昵称3')).toBeVisible()
})

test('admin login opens user management', async ({ page }) => {
  await login(page, 'admin', '123456')
  await page.waitForURL(/\/admin/)
  await page.goto(`${BASE_URL}/admin/users`)
  await expect(page.getByText('新增用户')).toBeVisible()
  await expect(page.getByText('临时用户')).toBeVisible()
})
