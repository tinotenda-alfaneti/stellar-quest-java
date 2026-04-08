const consoleOutput = document.getElementById('consoleOutput');
const configMeta = document.getElementById('configMeta');
const clearConsoleBtn = document.getElementById('clearConsole');

function timestamp() {
  return new Date().toLocaleTimeString();
}

function writeLog(title, payload, isError = false) {
  const prefix = isError ? '[ERROR]' : '[OK]';
  const body = typeof payload === 'string' ? payload : JSON.stringify(payload, null, 2);
  const entry = `${prefix} ${timestamp()} ${title}\n${body}\n\n`;

  if (consoleOutput.textContent === 'Waiting for action...') {
    consoleOutput.textContent = entry;
  } else {
    consoleOutput.textContent = entry + consoleOutput.textContent;
  }
}

function toJson(form) {
  const data = {};
  const elements = form.querySelectorAll('input, select, textarea');

  elements.forEach((el) => {
    const name = el.name;
    if (!name) {
      return;
    }

    if (el.type === 'checkbox') {
      data[name] = el.checked;
      return;
    }

    const value = (el.value || '').trim();
    if (value !== '') {
      data[name] = value;
    }
  });

  return data;
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
    ...options,
  });

  let payload;
  try {
    payload = await response.json();
  } catch (_e) {
    throw new Error(`Unexpected response (${response.status}).`);
  }

  if (!response.ok || payload.success === false) {
    throw new Error(payload.message || `Request failed (${response.status}).`);
  }

  return payload;
}

function bindPostForm(formId, endpoint, title) {
  const form = document.getElementById(formId);
  if (!form) {
    return;
  }

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const submitButton = form.querySelector('button[type="submit"]');
    submitButton.disabled = true;

    try {
      const payload = toJson(form);
      const result = await api(endpoint, {
        method: 'POST',
        body: JSON.stringify(payload),
      });
      writeLog(title, result);
    } catch (error) {
      writeLog(title, error.message, true);
    } finally {
      submitButton.disabled = false;
    }
  });
}

async function loadConfig() {
  try {
    const payload = await api('/api/config');
    const data = payload.data || {};
    const badges = [
      `Network: ${data.network || 'unknown'}`,
      `Horizon: ${data.horizonUrl || 'n/a'}`,
      `Quest Account: ${data.questAccountId || 'not configured'}`,
    ];

    configMeta.innerHTML = badges
      .map((label) => `<span class="meta-badge">${label}</span>`)
      .join('');

    const defaults = data.defaults || {};
    const createBalance = document.querySelector('#createAccountForm input[name="startingBalance"]');
    const paymentAmount = document.querySelector('#paymentForm input[name="paymentAmount"]');
    const mergeDestination = document.querySelector('#accountMergeForm input[name="destinationPublicKey"]');
    const trustAsset = document.querySelector('#trustlineForm input[name="assetCode"]');
    const trustLimit = document.querySelector('#trustlineForm input[name="trustLimit"]');
    const offerType = document.querySelector('#offerForm select[name="offerType"]');
    const offerAssetCode = document.querySelector('#offerForm input[name="offerAssetCode"]');
    const offerAssetIssuer = document.querySelector('#offerForm input[name="offerAssetIssuer"]');
    const offerPrice = document.querySelector('#offerForm input[name="offerPrice"]');
    const offerAmount = document.querySelector('#offerForm input[name="offerAmount"]');
    const offerBuyAmount = document.querySelector('#offerForm input[name="offerBuyAmount"]');
    const offerId = document.querySelector('#offerForm input[name="offerId"]');
    const offerTrustLimit = document.querySelector('#offerForm input[name="offerTrustLimit"]');
    const pathAssetCode = document.querySelector('#pathPaymentForm input[name="pathAssetCode"]');
    const pathSendAmount = document.querySelector('#pathPaymentForm input[name="pathSendAmount"]');
    const pathDestMin = document.querySelector('#pathPaymentForm input[name="pathDestMin"]');

    if (createBalance && defaults.startingBalance) createBalance.value = defaults.startingBalance;
    if (paymentAmount && defaults.paymentAmount) paymentAmount.value = defaults.paymentAmount;
    if (mergeDestination && defaults.mergeDestinationPublicKey) mergeDestination.value = defaults.mergeDestinationPublicKey;
    if (trustAsset && defaults.assetCode) trustAsset.value = defaults.assetCode;
    if (trustLimit && defaults.trustLimit) trustLimit.value = defaults.trustLimit;
    if (offerType && defaults.offerType) offerType.value = defaults.offerType;
    if (offerAssetCode && defaults.offerAssetCode) offerAssetCode.value = defaults.offerAssetCode;
    if (offerAssetIssuer && defaults.offerAssetIssuer) offerAssetIssuer.value = defaults.offerAssetIssuer;
    if (offerPrice && defaults.offerPrice) offerPrice.value = defaults.offerPrice;
    if (offerAmount && defaults.offerAmount) offerAmount.value = defaults.offerAmount;
    if (offerBuyAmount && defaults.offerBuyAmount) offerBuyAmount.value = defaults.offerBuyAmount;
    if (offerId && defaults.offerId) offerId.value = defaults.offerId;
    if (offerTrustLimit && defaults.offerTrustLimit) offerTrustLimit.value = defaults.offerTrustLimit;
    if (pathAssetCode && defaults.pathAssetCode) pathAssetCode.value = defaults.pathAssetCode;
    if (pathSendAmount && defaults.pathSendAmount) pathSendAmount.value = defaults.pathSendAmount;
    if (pathDestMin && defaults.pathDestMin) pathDestMin.value = defaults.pathDestMin;
  } catch (error) {
    configMeta.innerHTML = '<span class="meta-badge">Config unavailable</span>';
    writeLog('Config', error.message, true);
  }
}

clearConsoleBtn.addEventListener('click', () => {
  consoleOutput.textContent = 'Waiting for action...';
});

bindPostForm('createAccountForm', '/api/transactions/create-account', 'Create account transaction');
bindPostForm('paymentForm', '/api/transactions/payment', 'Payment transaction');
bindPostForm('accountMergeForm', '/api/transactions/account-merge', 'Account merge transaction');
bindPostForm('trustlineForm', '/api/transactions/trustline', 'Trustline transaction');
bindPostForm('offerForm', '/api/transactions/offer', 'Offer transaction');
bindPostForm('pathPaymentForm', '/api/transactions/path-payment', 'Path payment transaction');
bindPostForm('fundForm', '/api/transactions/fund', 'Friendbot fund');
loadConfig();
