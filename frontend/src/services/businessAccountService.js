import api from './api'

function extractAccountList(responseData) {
  console.log('API RAW BUSINESS ACCOUNTS:', responseData)

  // Direct array response:
  // [
  //   { id: 1, companyName: "ABC Logistics" }
  // ]
  if (Array.isArray(responseData)) {
    return responseData
  }

  // Spring Page response:
  // {
  //   content: [
  //     { id: 1, companyName: "ABC Logistics" }
  //   ]
  // }
  if (Array.isArray(responseData?.content)) {
    return responseData.content
  }

  // Wrapped response:
  // {
  //   data: [
  //     { id: 1, companyName: "ABC Logistics" }
  //   ]
  // }
  if (Array.isArray(responseData?.data)) {
    return responseData.data
  }

  // Another possible wrapper:
  // {
  //   accounts: [
  //     { id: 1, companyName: "ABC Logistics" }
  //   ]
  // }
  if (Array.isArray(responseData?.accounts)) {
    return responseData.accounts
  }

  console.error(
    'Business accounts response does not contain an array:',
    responseData
  )

  return []
}

export const businessAccountService = {
  getMine: async () => {
    const response = await api.get('/business-accounts/me')
    return response.data
  },

  create: async (payload) => {
    const response = await api.post('/business-accounts', payload)
    return response.data
  },

  updateMine: async (payload) => {
    const response = await api.put('/business-accounts/me', payload)
    return response.data
  },

  list: async () => {
    const response = await api.get('/business-accounts')

    const accounts = extractAccountList(response.data)

    console.log('FINAL BUSINESS ACCOUNT LIST:', accounts)

    return accounts
  },
}