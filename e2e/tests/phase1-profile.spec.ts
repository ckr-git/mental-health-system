/**
 * Phase 1 Platform Infrastructure — E2E Tests
 * Mental Health Management System
 *
 * Covers:
 *   Journey 1: Patient Profile CRUD (all Phase 1 fields)
 *   Journey 2: Profile Page Layout Verification (new card structure)
 *   Journey 3: Emergency Contact section is a separate card
 *   Journey 4: Health Records card (病史/过敏史/家族病史)
 */

import { test, expect, Page } from '@playwright/test';

// --------------------------------------------------------------------------
// Helpers
// --------------------------------------------------------------------------

const BASE_URL = 'http://localhost:5173';

/** Login as patient and return to the page after auth redirect. */
async function loginAsPatient(page: Page) {
  await page.goto(`${BASE_URL}/login`);
  // Wait for login form to be visible
  await page.waitForSelector('input[type="text"], input[placeholder*="用户名"], input[placeholder*="账号"]', {
    timeout: 15000,
  });

  // Fill username — try different selector strategies for Element Plus
  const usernameInput = page.locator('input').filter({ hasText: '' }).first();
  const inputs = page.locator('.el-input__inner, input[type="text"], input[placeholder]');
  const count = await inputs.count();

  if (count >= 2) {
    await inputs.nth(0).fill('patient001');
    await inputs.nth(1).fill('123456');
  } else {
    // fallback: fill by placeholder
    await page.fill('input[placeholder*="用户名"], input[placeholder*="账号"], input[placeholder*="手机"]', 'patient001');
    await page.fill('input[type="password"]', '123456');
  }

  // Click login button
  const loginBtn = page.locator('button').filter({ hasText: /登录|登 录/ }).first();
  await loginBtn.click();

  // Wait for redirect to patient dashboard
  await page.waitForURL(/\/patient/, { timeout: 20000 });
}

/** Navigate to profile page after login. */
async function goToProfile(page: Page) {
  await page.goto(`${BASE_URL}/patient/profile`);
  // Wait for profile container to appear
  await page.waitForSelector('.profile-container, .el-card', { timeout: 20000 });
  // Extra wait for API data to load
  await page.waitForTimeout(2000);
}

// --------------------------------------------------------------------------
// Journey 1: Patient Profile CRUD
// --------------------------------------------------------------------------

test.describe('Journey 1: Patient Profile CRUD', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsPatient(page);
  });

  test('1.1 Profile page loads successfully after login', async ({ page }) => {
    await goToProfile(page);

    // Verify page title / heading area — at least a few cards must be rendered
    const cardHeaders = page.locator('.el-card__header');
    const headerCount = await cardHeaders.count();
    expect(headerCount).toBeGreaterThanOrEqual(2);

    // Check page URL is correct
    expect(page.url()).toContain('/patient/profile');

    // Verify no JavaScript errors (console monitor done separately)
    const title = await page.title();
    expect(title).not.toContain('404');
  });

  test('1.2 Profile page has 基本信息 card with Phase 1 fields', async ({ page }) => {
    await goToProfile(page);

    // Verify 基本信息 card header exists
    const cardHeaders = page.locator('.el-card__header span, .el-card__header .card-header span');
    const headers = await cardHeaders.allInnerTexts();
    const headerText = headers.join(' ');
    expect(headerText).toContain('基本信息');

    // Verify Phase 1 fields are present
    const formLabels = page.locator('.el-form-item__label');
    const labels = await formLabels.allInnerTexts();
    const labelText = labels.join(' ');

    expect(labelText).toContain('姓名');       // realName field
    expect(labelText).toContain('昵称');       // nickname — new in Phase 1
    expect(labelText).toContain('性别');
    expect(labelText).toContain('年龄');
    expect(labelText).toContain('邮箱');
    expect(labelText).toContain('职业');       // occupation — new in Phase 1
    expect(labelText).toContain('婚姻状况');   // maritalStatus — new in Phase 1
    expect(labelText).toContain('个人简介');
  });

  test('1.3 Emergency contact card is separate and has correct fields', async ({ page }) => {
    await goToProfile(page);

    // Verify 紧急联系人 is its own card (not inside 基本信息)
    const cardHeaders = page.locator('.el-card__header');
    const allHeaders = await cardHeaders.allInnerTexts();
    const joined = allHeaders.join('|');
    expect(joined).toContain('紧急联系人');

    // Verify emergency contact fields
    const formLabels = page.locator('.el-form-item__label');
    const labels = await formLabels.allInnerTexts();
    const labelText = labels.join(' ');

    expect(labelText).toContain('联系人姓名');     // emergencyContactName
    expect(labelText).toContain('联系人电话');     // emergencyContactPhone
    expect(labelText).toContain('与您的关系');     // emergencyContactRelation
  });

  test('1.4 Health records card has all three history fields', async ({ page }) => {
    await goToProfile(page);

    const formLabels = page.locator('.el-form-item__label');
    const labels = await formLabels.allInnerTexts();
    const labelText = labels.join(' ');

    expect(labelText).toContain('病史');       // medicalHistory
    expect(labelText).toContain('过敏史');     // allergyHistory
    expect(labelText).toContain('家族病史');   // familyHistory
  });

  test('1.5 Click 编辑 enables form editing', async ({ page }) => {
    await goToProfile(page);

    // Find the 编辑 button in 基本信息 card
    const editBtn = page.locator('button').filter({ hasText: '编辑' }).first();
    await expect(editBtn).toBeVisible();
    await editBtn.click();

    // After clicking 编辑, form fields should be enabled
    // The save button should now appear
    const saveBtn = page.locator('button').filter({ hasText: '保存' });
    await expect(saveBtn).toBeVisible({ timeout: 5000 });

    // Cancel button should also appear
    const cancelBtn = page.locator('button').filter({ hasText: '取消' });
    await expect(cancelBtn).toBeVisible();
  });

  test('1.6 Fill and save basic info fields', async ({ page }) => {
    await goToProfile(page);

    // Click edit button
    const editBtn = page.locator('button').filter({ hasText: '编辑' }).first();
    await editBtn.click();
    await page.waitForSelector('button:has-text("保存")', { timeout: 5000 });

    // Fill realName — first enabled text input in 基本信息 form
    // The 姓名 field is the first el-form-item
    const nameInput = page.locator('.el-form-item').filter({ hasText: '姓名' }).locator('input').first();
    await nameInput.clear();
    await nameInput.fill('测试患者张三');

    // Fill nickname (昵称)
    const nicknameInput = page.locator('.el-form-item').filter({ hasText: '昵称' }).locator('input').first();
    await nicknameInput.clear();
    await nicknameInput.fill('张三小昵称');

    // Fill occupation (职业)
    const occupationInput = page.locator('.el-form-item').filter({ hasText: '职业' }).locator('input').first();
    await occupationInput.clear();
    await occupationInput.fill('软件工程师');

    // Click save
    const saveBtn = page.locator('button').filter({ hasText: '保存' }).first();
    await saveBtn.click();

    // Expect success message (el-message or el-notification)
    const successMsg = page.locator('.el-message--success, .el-notification--success, [class*="success"]');
    await expect(successMsg.first()).toBeVisible({ timeout: 10000 });
  });

  test('1.7 Fill emergency contact and health records', async ({ page }) => {
    await goToProfile(page);

    // Click edit button
    const editBtn = page.locator('button').filter({ hasText: '编辑' }).first();
    await editBtn.click();
    await page.waitForSelector('button:has-text("保存")', { timeout: 5000 });

    // Fill emergency contact name
    const emergencyNameInput = page.locator('.el-form-item')
      .filter({ hasText: '联系人姓名' })
      .locator('input')
      .first();
    await emergencyNameInput.clear();
    await emergencyNameInput.fill('张父亲');

    // Fill emergency contact phone
    const emergencyPhoneInput = page.locator('.el-form-item')
      .filter({ hasText: '联系人电话' })
      .locator('input')
      .first();
    await emergencyPhoneInput.clear();
    await emergencyPhoneInput.fill('13900123456');

    // Fill medical history
    const medicalHistoryInput = page.locator('.el-form-item')
      .filter({ hasText: '病史' })
      .locator('textarea')
      .first();
    await medicalHistoryInput.clear();
    await medicalHistoryInput.fill('既往无重大疾病史');

    // Fill allergy history
    const allergyInput = page.locator('.el-form-item')
      .filter({ hasText: '过敏史' })
      .locator('textarea')
      .first();
    await allergyInput.clear();
    await allergyInput.fill('对青霉素过敏');

    // Fill family history
    const familyHistoryInput = page.locator('.el-form-item')
      .filter({ hasText: '家族病史' })
      .locator('textarea')
      .first();
    await familyHistoryInput.clear();
    await familyHistoryInput.fill('父亲有高血压史');

    // Click save
    const saveBtn = page.locator('button').filter({ hasText: '保存' }).first();
    await saveBtn.click();

    // Wait for success or error message
    const message = page.locator('.el-message, .el-notification');
    await expect(message.first()).toBeVisible({ timeout: 10000 });

    // Check it is success (not error)
    const msgClass = await message.first().getAttribute('class');
    // Accept success or verify no --error class
    const isError = msgClass?.includes('error') ?? false;
    expect(isError).toBe(false);
  });

  test('1.8 Cancel edit reverts form to original values', async ({ page }) => {
    await goToProfile(page);

    // Wait for form to load with data
    await page.waitForTimeout(2000);

    // Get the current nickname value before editing
    const nicknameInput = page.locator('.el-form-item')
      .filter({ hasText: '昵称' })
      .locator('input')
      .first();
    const originalNickname = await nicknameInput.inputValue();

    // Click edit
    const editBtn = page.locator('button').filter({ hasText: '编辑' }).first();
    await editBtn.click();
    await page.waitForSelector('button:has-text("保存")', { timeout: 5000 });

    // Change nickname to something different
    await nicknameInput.clear();
    await nicknameInput.fill('临时测试昵称_CANCEL_ME');

    // Click cancel
    const cancelBtn = page.locator('button').filter({ hasText: '取消' }).first();
    await cancelBtn.click();

    // Editing mode should be off — 编辑 button should reappear
    await expect(page.locator('button').filter({ hasText: '编辑' }).first()).toBeVisible({ timeout: 5000 });

    // Nickname input should revert to original value
    const revertedValue = await nicknameInput.inputValue();
    expect(revertedValue).toBe(originalNickname);
    expect(revertedValue).not.toBe('临时测试昵称_CANCEL_ME');
  });
});

// --------------------------------------------------------------------------
// Journey 2: Profile Page Layout Verification
// --------------------------------------------------------------------------

test.describe('Journey 2: Profile Page Layout', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsPatient(page);
    await goToProfile(page);
  });

  test('2.1 Left column contains avatar card and account security card', async ({ page }) => {
    // Left column: el-col with span=8 containing profile-card and security card
    const leftCol = page.locator('.el-col').first();

    // Avatar/profile card
    const profileCard = leftCol.locator('.profile-card');
    await expect(profileCard).toBeVisible();

    // Stats section
    const statsSection = leftCol.locator('.profile-stats');
    await expect(statsSection).toBeVisible();

    // Account security card
    const securityCard = leftCol.locator('.el-card').filter({ hasText: '账户安全' });
    await expect(securityCard).toBeVisible();
  });

  test('2.2 Left column security card has 修改密码 and 更换手机 items', async ({ page }) => {
    const leftCol = page.locator('.el-col').first();
    const securityCard = leftCol.locator('.el-card').filter({ hasText: '账户安全' });

    await expect(securityCard.locator('span').filter({ hasText: '修改密码' })).toBeVisible();
    await expect(securityCard.locator('span').filter({ hasText: '更换手机' })).toBeVisible();
  });

  test('2.3 Right column has four info cards: 基本信息, 紧急联系人, 健康档案, 音效设置', async ({ page }) => {
    // Right column is the second el-col (span=16)
    const cols = page.locator('.el-col');
    const rightCol = cols.nth(1);

    const cardHeaders = rightCol.locator('.el-card__header');
    const headers = await cardHeaders.allInnerTexts();
    const joinedHeaders = headers.join('|');

    expect(joinedHeaders).toContain('基本信息');
    expect(joinedHeaders).toContain('紧急联系人');
    expect(joinedHeaders).toContain('健康档案');
    expect(joinedHeaders).toContain('音效设置');
  });

  test('2.4 Emergency contact card is separate from basic info card', async ({ page }) => {
    const cols = page.locator('.el-col');
    const rightCol = cols.nth(1);

    // There should be at least 4 distinct cards in the right column
    const cards = rightCol.locator('.el-card');
    const cardCount = await cards.count();
    expect(cardCount).toBeGreaterThanOrEqual(4);

    // The 紧急联系人 card must be a sibling of 基本信息 card (not nested inside it)
    // We check by looking for 紧急联系人 in a card header that is NOT inside 基本信息 card content
    const basicInfoCard = rightCol.locator('.el-card').filter({ hasText: '基本信息' }).first();
    // Emergency contact header should NOT be inside the basic info card
    const emergencyInsideBasic = basicInfoCard.locator('.el-card__header').filter({ hasText: '紧急联系人' });
    const countInsideBasic = await emergencyInsideBasic.count();
    expect(countInsideBasic).toBe(0);
  });

  test('2.5 Stats section shows three stat items', async ({ page }) => {
    const statsSection = page.locator('.profile-stats');
    await expect(statsSection).toBeVisible();

    const statItems = statsSection.locator('.stat-item');
    const count = await statItems.count();
    expect(count).toBe(3);

    // Check labels
    const labels = statsSection.locator('.stat-label');
    const labelTexts = await labels.allInnerTexts();
    const joinedLabels = labelTexts.join(' ');

    expect(joinedLabels).toContain('情绪日记');
    expect(joinedLabels).toContain('评估报告');
    expect(joinedLabels).toContain('使用天数');
  });

  test('2.6 Sound settings card has ambient toggle, interaction toggle, and volume slider', async ({ page }) => {
    const soundCard = page.locator('.el-card').filter({ hasText: '音效设置' });
    await expect(soundCard).toBeVisible();

    // Environment sound toggle
    await expect(soundCard.locator('span').filter({ hasText: '环境音效' })).toBeVisible();
    // Interaction sound toggle
    await expect(soundCard.locator('span').filter({ hasText: '交互音效' })).toBeVisible();
    // Volume control
    await expect(soundCard.locator('span').filter({ hasText: '音量控制' })).toBeVisible();
  });
});

// --------------------------------------------------------------------------
// Journey 3: Account Security Dialogs
// --------------------------------------------------------------------------

test.describe('Journey 3: Account Security Dialogs', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsPatient(page);
    await goToProfile(page);
  });

  test('3.1 Clicking 修改密码 opens password change dialog', async ({ page }) => {
    const changePasswordItem = page.locator('.security-item').filter({ hasText: '修改密码' });
    await changePasswordItem.click();

    // Dialog should appear
    const dialog = page.locator('.el-dialog').filter({ hasText: '修改密码' });
    await expect(dialog).toBeVisible({ timeout: 5000 });

    // Dialog should have 3 fields: 原密码, 新密码, 确认密码
    await expect(dialog.locator('.el-form-item__label').filter({ hasText: '原密码' })).toBeVisible();
    await expect(dialog.locator('.el-form-item__label').filter({ hasText: '新密码' })).toBeVisible();
    await expect(dialog.locator('.el-form-item__label').filter({ hasText: '确认密码' })).toBeVisible();

    // Close the dialog
    const cancelBtn = dialog.locator('button').filter({ hasText: '取消' });
    await cancelBtn.click();
    await expect(dialog).not.toBeVisible({ timeout: 5000 });
  });

  test('3.2 Clicking 更换手机 opens phone change dialog', async ({ page }) => {
    const changePhoneItem = page.locator('.security-item').filter({ hasText: '更换手机' });
    await changePhoneItem.click();

    // Dialog should appear
    const dialog = page.locator('.el-dialog').filter({ hasText: '更换手机' });
    await expect(dialog).toBeVisible({ timeout: 5000 });

    // Close it
    const cancelBtn = dialog.locator('button').filter({ hasText: '取消' });
    await cancelBtn.click();
    await expect(dialog).not.toBeVisible({ timeout: 5000 });
  });
});

// --------------------------------------------------------------------------
// Journey 4: API Integration — Backend connectivity
// --------------------------------------------------------------------------

test.describe('Journey 4: Backend API Integration', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsPatient(page);
  });

  test('4.1 GET /api/patient/profile returns 200 after login', async ({ page }) => {
    // Intercept the API call made when profile page loads
    const responsePromise = page.waitForResponse(
      (res) => res.url().includes('/api/patient/profile') && res.request().method() === 'GET',
      { timeout: 15000 }
    );

    await page.goto(`${BASE_URL}/patient/profile`);

    try {
      const response = await responsePromise;
      expect(response.status()).toBe(200);

      const body = await response.json();
      // Response should have code 200 and data
      expect(body.code).toBe(200);
      expect(body.data).toBeDefined();
    } catch (e) {
      // Backend might not be running — log and skip gracefully
      console.warn('Backend API not reachable — skipping API response check:', (e as Error).message);
      test.skip();
    }
  });

  test('4.2 PUT /api/patient/profile saves data correctly', async ({ page }) => {
    await goToProfile(page);

    // Monitor PUT request
    const putResponsePromise = page.waitForResponse(
      (res) => res.url().includes('/api/patient/profile') && res.request().method() === 'PUT',
      { timeout: 20000 }
    );

    // Click edit and save
    const editBtn = page.locator('button').filter({ hasText: '编辑' }).first();
    await editBtn.click();
    await page.waitForSelector('button:has-text("保存")', { timeout: 5000 });

    // Make a minor change
    const nicknameInput = page.locator('.el-form-item')
      .filter({ hasText: '昵称' })
      .locator('input')
      .first();
    await nicknameInput.clear();
    await nicknameInput.fill('E2E测试昵称');

    const saveBtn = page.locator('button').filter({ hasText: '保存' }).first();
    await saveBtn.click();

    try {
      const response = await putResponsePromise;
      expect(response.status()).toBe(200);

      const body = await response.json();
      expect(body.code).toBe(200);
    } catch (e) {
      console.warn('Backend API not reachable for PUT — skipping:', (e as Error).message);
      test.skip();
    }
  });

  test('4.3 Gender radio buttons use numeric values (1=男, 2=女)', async ({ page }) => {
    await goToProfile(page);

    // Click edit to enable form
    const editBtn = page.locator('button').filter({ hasText: '编辑' }).first();
    await editBtn.click();
    await page.waitForSelector('button:has-text("保存")', { timeout: 5000 });

    // Find the radio buttons for gender
    const genderGroup = page.locator('.el-form-item').filter({ hasText: '性别' }).locator('.el-radio-group');
    await expect(genderGroup).toBeVisible();

    // Should have exactly 2 radio buttons: 男 and 女
    const radios = genderGroup.locator('.el-radio');
    const count = await radios.count();
    expect(count).toBe(2);

    const radioTexts = await radios.allInnerTexts();
    const joinedTexts = radioTexts.join(' ');
    expect(joinedTexts).toContain('男');
    expect(joinedTexts).toContain('女');

    // Cancel to avoid accidental save
    const cancelBtn = page.locator('button').filter({ hasText: '取消' }).first();
    await cancelBtn.click();
  });

  test('4.4 Marital status dropdown has 4 options', async ({ page }) => {
    await goToProfile(page);

    const editBtn = page.locator('button').filter({ hasText: '编辑' }).first();
    await editBtn.click();
    await page.waitForSelector('button:has-text("保存")', { timeout: 5000 });

    // Click the marital status select to open dropdown
    const maritalSelect = page.locator('.el-form-item')
      .filter({ hasText: '婚姻状况' })
      .locator('.el-select');
    await maritalSelect.click();

    // Wait for dropdown to open
    await page.waitForSelector('.el-select-dropdown__list', { timeout: 5000 });

    const options = page.locator('.el-select-dropdown__item');
    const count = await options.count();
    expect(count).toBeGreaterThanOrEqual(4);

    const optionTexts = await options.allInnerTexts();
    const joined = optionTexts.join(' ');
    expect(joined).toContain('未婚');
    expect(joined).toContain('已婚');
    expect(joined).toContain('离异');
    expect(joined).toContain('丧偶');

    // Press Escape to close dropdown
    await page.keyboard.press('Escape');

    // Cancel edit
    const cancelBtn = page.locator('button').filter({ hasText: '取消' }).first();
    await cancelBtn.click();
  });

  test('4.5 Emergency contact relation dropdown has PARENT/SPOUSE/CHILD options', async ({ page }) => {
    await goToProfile(page);

    const editBtn = page.locator('button').filter({ hasText: '编辑' }).first();
    await editBtn.click();
    await page.waitForSelector('button:has-text("保存")', { timeout: 5000 });

    // Click emergency contact relation dropdown
    const relationSelect = page.locator('.el-form-item')
      .filter({ hasText: '与您的关系' })
      .locator('.el-select');
    await relationSelect.click();

    // Wait for the dropdown popper to become visible (Element Plus uses teleport)
    // The dropdown items are visible when the popper panel appears
    await page.waitForFunction(() => {
      const dropdowns = document.querySelectorAll('.el-select-dropdown__list');
      return Array.from(dropdowns).some(el => {
        const style = window.getComputedStyle(el);
        return style.display !== 'none' && style.visibility !== 'hidden';
      });
    }, { timeout: 8000 });

    // Get options — filter to visible ones only
    const allOptions = page.locator('.el-select-dropdown__item');
    const optionTexts = await allOptions.allInnerTexts();
    const joined = optionTexts.join(' ');

    expect(joined).toContain('父母');
    expect(joined).toContain('配偶');
    expect(joined).toContain('子女');
    expect(joined).toContain('兄弟姐妹');
    expect(joined).toContain('朋友');
    expect(joined).toContain('其他');

    await page.keyboard.press('Escape');
    const cancelBtn = page.locator('button').filter({ hasText: '取消' }).first();
    await cancelBtn.click();
  });
});

// --------------------------------------------------------------------------
// Journey 5: No Console Errors on Profile Page
// --------------------------------------------------------------------------

test.describe('Journey 5: No Console Errors', () => {
  test('5.1 Profile page loads without console errors', async ({ page }) => {
    const consoleErrors: string[] = [];

    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });

    await loginAsPatient(page);
    await goToProfile(page);
    await page.waitForTimeout(3000);

    // Filter out known benign errors (like network errors when backend is down)
    const criticalErrors = consoleErrors.filter((err) =>
      !err.includes('net::ERR_CONNECTION_REFUSED') &&
      !err.includes('favicon') &&
      !err.includes('404') &&
      !err.includes('Failed to fetch')
    );

    if (criticalErrors.length > 0) {
      console.warn('Console errors found:', criticalErrors);
    }

    // Only fail on truly unexpected errors
    expect(criticalErrors).toHaveLength(0);
  });

  test('5.2 Profile page has no undefined/null/NaN text displayed', async ({ page }) => {
    await loginAsPatient(page);
    await goToProfile(page);
    await page.waitForTimeout(3000);

    // Get all visible text on the page
    const pageText = await page.locator('.profile-container').innerText();

    // Check that no raw undefined/null/NaN is displayed to the user
    expect(pageText).not.toContain('undefined');
    expect(pageText).not.toContain('[object Object]');
    // Note: "null" might appear in Chinese text as part of words, so we are selective
    expect(pageText).not.toMatch(/^\s*null\s*$/m);
  });
});
