/**
 * Axios API Client
 *
 * Central API client configuration with interceptors and global error handling.
 *
 * @module api/client
 */
import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios';
import { type ErrorResponse, type FieldError } from '@/types';

/**
 * Custom error class for API errors with structured error data.
 */
export class ApiError extends Error {
  public readonly status: number;
  public readonly errorType: string;
  public readonly path: string;
  public readonly timestamp: string;
  public readonly fieldErrors?: FieldError[];

  constructor(errorResponse: ErrorResponse) {
    super(errorResponse.message);
    this.name = 'ApiError';
    this.status = errorResponse.status;
    this.errorType = errorResponse.error;
    this.path = errorResponse.path;
    this.timestamp = errorResponse.timestamp;
    this.fieldErrors = errorResponse.fieldErrors;
  }
}

/**
 * Base URL for the API.
 * Uses localhost:8080 as defined in API-Contract.md
 */
const BASE_URL = 'http://localhost:8080/api';

/**
 * Request timeout in milliseconds.
 */
const REQUEST_TIMEOUT = 30000;

/**
 * Create and configure the Axios instance.
 */
const createApiClient = (): AxiosInstance => {
  const instance = axios.create({
    baseURL: BASE_URL,
    timeout: REQUEST_TIMEOUT,
    headers: {
      Accept: 'application/json',
    },
  });

  /**
   * Request interceptor - adds Content-Type header for requests with body.
   */
  instance.interceptors.request.use(
    (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
      // Add Content-Type for POST/PUT/PATCH requests with JSON body
      if (config.data && !isFormData(config.data)) {
        config.headers.set('Content-Type', 'application/json');
      }
      return config;
    },
    (error: AxiosError): Promise<never> => {
      return Promise.reject(error);
    }
  );

  /**
   * Response interceptor - handles successful responses and transforms errors.
   */
  instance.interceptors.response.use(
    (response: AxiosResponse): AxiosResponse => {
      return response;
    },
    (error: AxiosError<ErrorResponse>): Promise<never> => {
      return Promise.reject(transformError(error));
    }
  );

  return instance;
};

/**
 * Check if the data is FormData (for multipart uploads).
 * @internal Exported for testing purposes
 */
export const isFormData = (data: unknown): data is FormData => {
  return typeof FormData !== 'undefined' && data instanceof FormData;
};

/**
 * Transform Axios error into a structured ApiError or preserve network errors.
 * @internal Exported for testing purposes
 */
export const transformError = (error: AxiosError<ErrorResponse>): Error => {
  // Handle network errors (no response received)
  if (!error.response) {
    const networkError = new Error(
      error.message || 'Network error - unable to connect to the server'
    );
    networkError.name = 'NetworkError';
    return networkError;
  }

  // Handle structured API error responses
  const { data, status } = error.response;

  if (data && typeof data === 'object' && 'message' in data) {
    return new ApiError({
      timestamp: data.timestamp ?? new Date().toISOString(),
      status: data.status ?? status,
      error: data.error ?? getDefaultErrorType(status),
      message: data.message,
      path: data.path ?? error.config?.url ?? '',
      fieldErrors: data.fieldErrors,
    });
  }

  // Handle non-standard error responses
  return new ApiError({
    timestamp: new Date().toISOString(),
    status,
    error: getDefaultErrorType(status),
    message: error.message || 'An unexpected error occurred',
    path: error.config?.url ?? '',
  });
};

/**
 * Get default error type string based on HTTP status code.
 * @internal Exported for testing purposes
 */
export const getDefaultErrorType = (status: number): string => {
  switch (status) {
    case 400:
      return 'Bad Request';
    case 401:
      return 'Unauthorized';
    case 403:
      return 'Forbidden';
    case 404:
      return 'Not Found';
    case 409:
      return 'Conflict';
    case 500:
      return 'Internal Server Error';
    case 503:
      return 'Service Unavailable';
    default:
      return 'Error';
  }
};

/**
 * The configured Axios instance for API calls.
 */
export const apiClient = createApiClient();

/**
 * Convenience methods for common HTTP operations.
 */
export const api = {
  /**
   * Perform a GET request.
   * @param url - The endpoint URL
   * @param config - Optional Axios request config
   * @returns Promise resolving to the response data
   */
  get: <T>(url: string, config?: Parameters<typeof apiClient.get>[1]): Promise<T> =>
    apiClient.get<T>(url, config).then((res) => res.data),

  /**
   * Perform a POST request.
   * @param url - The endpoint URL
   * @param data - The request body data
   * @param config - Optional Axios request config
   * @returns Promise resolving to the response data
   */
  post: <T>(
    url: string,
    data?: unknown,
    config?: Parameters<typeof apiClient.post>[2]
  ): Promise<T> => apiClient.post<T>(url, data, config).then((res) => res.data),

  /**
   * Perform a PUT request.
   * @param url - The endpoint URL
   * @param data - The request body data
   * @param config - Optional Axios request config
   * @returns Promise resolving to the response data
   */
  put: <T>(url: string, data?: unknown, config?: Parameters<typeof apiClient.put>[2]): Promise<T> =>
    apiClient.put<T>(url, data, config).then((res) => res.data),

  /**
   * Perform a DELETE request.
   * @param url - The endpoint URL
   * @param config - Optional Axios request config
   * @returns Promise resolving to the response data
   */
  delete: <T>(url: string, config?: Parameters<typeof apiClient.delete>[1]): Promise<T> =>
    apiClient.delete<T>(url, config).then((res) => res.data),

  /**
   * Perform a PATCH request.
   * @param url - The endpoint URL
   * @param data - The request body data
   * @param config - Optional Axios request config
   * @returns Promise resolving to the response data
   */
  patch: <T>(
    url: string,
    data?: unknown,
    config?: Parameters<typeof apiClient.patch>[2]
  ): Promise<T> => apiClient.patch<T>(url, data, config).then((res) => res.data),
};
