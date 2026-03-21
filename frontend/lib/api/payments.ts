import { apiClient } from './client';

export interface PaymentPrepareRequest {
    orderId: number;
}

export interface CreatePaymentResponse {
    orderKey: string;
    amount: number;
}

export interface PaymentConfirmResponse {
    orderId: number;
    orderKey: string;
    status: 'SUCCEEDED';
}

export const paymentsApi = {
    preparePayment: async (orderId: number) => {
        const response = await apiClient.post('/payments', { orderId });
        return response.data as CreatePaymentResponse;
    },

    confirmPayment: async (params: { paymentKey: string; orderKey: string; amount: number }) => {
        const response = await apiClient.get('/payments/success', {
            params: {
                paymentKey: params.paymentKey,
                orderId: params.orderKey,
                amount: params.amount,
            }
        });

        return response.data as PaymentConfirmResponse;
    }
};