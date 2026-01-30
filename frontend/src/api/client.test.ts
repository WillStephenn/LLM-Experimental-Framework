/**
 * Tests for Axios API Client
 *
 * @module api/client.test
 */
import { describe, it, expect, beforeEach, afterEach, vi, type Mock } from 'vitest';
import { type AxiosError, type AxiosResponse } from 'axios';
import { type ErrorResponse } from '@/types';
import {
  api,
  apiClient,
  ApiError,
  isFormData,
  transformError,
  getDefaultErrorType,
} from './client';

describe('ApiError', () => {
  it('should create an ApiError with all properties', () => {
    const errorResponse: ErrorResponse = {
      timestamp: '2025-11-27T10:00:00Z',
      status: 400,
      error: 'Bad Request',
      message: 'Validation failed',
      path: '/api/tasks',
      fieldErrors: [{ field: 'name', message: 'must not be blank' }],
    };

    const error = new ApiError(errorResponse);

    expect(error.name).toBe('ApiError');
    expect(error.message).toBe('Validation failed');
    expect(error.status).toBe(400);
    expect(error.errorType).toBe('Bad Request');
    expect(error.path).toBe('/api/tasks');
    expect(error.timestamp).toBe('2025-11-27T10:00:00Z');
    expect(error.fieldErrors).toEqual([{ field: 'name', message: 'must not be blank' }]);
  });

  it('should create an ApiError without field errors', () => {
    const errorResponse: ErrorResponse = {
      timestamp: '2025-11-27T10:00:00Z',
      status: 404,
      error: 'Not Found',
      message: 'Resource not found',
      path: '/api/tasks/999',
    };

    const error = new ApiError(errorResponse);

    expect(error.status).toBe(404);
    expect(error.fieldErrors).toBeUndefined();
  });

  it('should be an instance of Error', () => {
    const errorResponse: ErrorResponse = {
      timestamp: '2025-11-27T10:00:00Z',
      status: 500,
      error: 'Internal Server Error',
      message: 'Something went wrong',
      path: '/api/tasks',
    };

    const error = new ApiError(errorResponse);

    expect(error).toBeInstanceOf(Error);
    expect(error).toBeInstanceOf(ApiError);
  });
});

describe('apiClient', () => {
  it('should have apiClient defined', () => {
    expect(apiClient).toBeDefined();
  });

  it('should have api convenience object defined with all methods', () => {
    expect(api).toBeDefined();
    expect(api.get).toBeDefined();
    expect(api.post).toBeDefined();
    expect(api.put).toBeDefined();
    expect(api.delete).toBeDefined();
    expect(api.patch).toBeDefined();
    expect(typeof api.get).toBe('function');
    expect(typeof api.post).toBe('function');
    expect(typeof api.put).toBe('function');
    expect(typeof api.delete).toBe('function');
    expect(typeof api.patch).toBe('function');
  });
});

describe('api convenience methods', () => {
  const mockResponse = {
    data: { id: 1, name: 'Test' },
  } as AxiosResponse<{ id: number; name: string }>;
  let getMock: Mock;
  let postMock: Mock;
  let putMock: Mock;
  let deleteMock: Mock;
  let patchMock: Mock;

  beforeEach(() => {
    vi.clearAllMocks();
    getMock = vi.fn().mockResolvedValue(mockResponse);
    postMock = vi.fn().mockResolvedValue(mockResponse);
    putMock = vi.fn().mockResolvedValue(mockResponse);
    deleteMock = vi.fn().mockResolvedValue(mockResponse);
    patchMock = vi.fn().mockResolvedValue(mockResponse);

    vi.spyOn(apiClient, 'get').mockImplementation(getMock);
    vi.spyOn(apiClient, 'post').mockImplementation(postMock);
    vi.spyOn(apiClient, 'put').mockImplementation(putMock);
    vi.spyOn(apiClient, 'delete').mockImplementation(deleteMock);
    vi.spyOn(apiClient, 'patch').mockImplementation(patchMock);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('api.get should call apiClient.get and return data', async () => {
    const result = await api.get<{ id: number; name: string }>('/tasks');

    expect(getMock).toHaveBeenCalledWith('/tasks', undefined);
    expect(result).toEqual({ id: 1, name: 'Test' });
  });

  it('api.get should pass config options', async () => {
    const config = { params: { page: 1 } };
    await api.get('/tasks', config);

    expect(getMock).toHaveBeenCalledWith('/tasks', config);
  });

  it('api.post should call apiClient.post and return data', async () => {
    const body = { name: 'New Task' };
    const result = await api.post<{ id: number; name: string }>('/tasks', body);

    expect(postMock).toHaveBeenCalledWith('/tasks', body, undefined);
    expect(result).toEqual({ id: 1, name: 'Test' });
  });

  it('api.put should call apiClient.put and return data', async () => {
    const body = { name: 'Updated Task' };
    const result = await api.put<{ id: number; name: string }>('/tasks/1', body);

    expect(putMock).toHaveBeenCalledWith('/tasks/1', body, undefined);
    expect(result).toEqual({ id: 1, name: 'Test' });
  });

  it('api.delete should call apiClient.delete and return data', async () => {
    const result = await api.delete<{ id: number; name: string }>('/tasks/1');

    expect(deleteMock).toHaveBeenCalledWith('/tasks/1', undefined);
    expect(result).toEqual({ id: 1, name: 'Test' });
  });

  it('api.patch should call apiClient.patch and return data', async () => {
    const body = { name: 'Patched Task' };
    const result = await api.patch<{ id: number; name: string }>('/tasks/1', body);

    expect(patchMock).toHaveBeenCalledWith('/tasks/1', body, undefined);
    expect(result).toEqual({ id: 1, name: 'Test' });
  });
});

describe('isFormData', () => {
  it('should return true for FormData instances', () => {
    const formData = new FormData();
    expect(isFormData(formData)).toBe(true);
  });

  it('should return false for plain objects', () => {
    expect(isFormData({ foo: 'bar' })).toBe(false);
  });

  it('should return false for null', () => {
    expect(isFormData(null)).toBe(false);
  });

  it('should return false for undefined', () => {
    expect(isFormData(undefined)).toBe(false);
  });

  it('should return false for strings', () => {
    expect(isFormData('string')).toBe(false);
  });

  it('should return false for arrays', () => {
    expect(isFormData([1, 2, 3])).toBe(false);
  });
});

describe('getDefaultErrorType', () => {
  it('should return "Bad Request" for status 400', () => {
    expect(getDefaultErrorType(400)).toBe('Bad Request');
  });

  it('should return "Unauthorized" for status 401', () => {
    expect(getDefaultErrorType(401)).toBe('Unauthorized');
  });

  it('should return "Forbidden" for status 403', () => {
    expect(getDefaultErrorType(403)).toBe('Forbidden');
  });

  it('should return "Not Found" for status 404', () => {
    expect(getDefaultErrorType(404)).toBe('Not Found');
  });

  it('should return "Conflict" for status 409', () => {
    expect(getDefaultErrorType(409)).toBe('Conflict');
  });

  it('should return "Internal Server Error" for status 500', () => {
    expect(getDefaultErrorType(500)).toBe('Internal Server Error');
  });

  it('should return "Service Unavailable" for status 503', () => {
    expect(getDefaultErrorType(503)).toBe('Service Unavailable');
  });

  it('should return "Error" for unknown status codes', () => {
    expect(getDefaultErrorType(418)).toBe('Error');
    expect(getDefaultErrorType(999)).toBe('Error');
  });
});

describe('transformError', () => {
  it('should transform network error without response', () => {
    const axiosError = {
      response: undefined,
      message: 'Network Error',
      config: { url: '/api/tasks' },
    } as unknown as AxiosError<ErrorResponse>;

    const error = transformError(axiosError);

    expect(error.name).toBe('NetworkError');
    expect(error.message).toBe('Network Error');
  });

  it('should use default message for network error without message', () => {
    const axiosError = {
      response: undefined,
      message: '',
      config: { url: '/api/tasks' },
    } as unknown as AxiosError<ErrorResponse>;

    const error = transformError(axiosError);

    expect(error.name).toBe('NetworkError');
    expect(error.message).toBe('Network error - unable to connect to the server');
  });

  it('should transform structured API error response to ApiError', () => {
    const axiosError = {
      response: {
        data: {
          timestamp: '2025-11-27T10:00:00Z',
          status: 400,
          error: 'Bad Request',
          message: 'Validation failed',
          path: '/api/tasks',
          fieldErrors: [{ field: 'name', message: 'must not be blank' }],
        },
        status: 400,
      },
      message: 'Request failed',
      config: { url: '/api/tasks' },
    } as unknown as AxiosError<ErrorResponse>;

    const error = transformError(axiosError);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(400);
    expect((error as ApiError).message).toBe('Validation failed');
    expect((error as ApiError).errorType).toBe('Bad Request');
    expect((error as ApiError).path).toBe('/api/tasks');
    expect((error as ApiError).fieldErrors).toHaveLength(1);
  });

  it('should use default values for missing fields in structured response', () => {
    const axiosError = {
      response: {
        data: {
          message: 'Something went wrong',
        },
        status: 500,
      },
      message: 'Request failed',
      config: { url: '/api/tasks' },
    } as unknown as AxiosError<ErrorResponse>;

    const error = transformError(axiosError);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(500);
    expect((error as ApiError).message).toBe('Something went wrong');
    expect((error as ApiError).errorType).toBe('Internal Server Error');
    expect((error as ApiError).path).toBe('/api/tasks');
    expect((error as ApiError).timestamp).toBeDefined();
  });

  it('should handle non-standard error response (string data)', () => {
    const axiosError = {
      response: {
        data: 'Internal Server Error',
        status: 500,
      },
      message: 'Request failed with status code 500',
      config: { url: '/api/tasks' },
    } as unknown as AxiosError<ErrorResponse>;

    const error = transformError(axiosError);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(500);
    expect((error as ApiError).message).toBe('Request failed with status code 500');
    expect((error as ApiError).errorType).toBe('Internal Server Error');
  });

  it('should handle error response without config URL', () => {
    const axiosError = {
      response: {
        data: {
          message: 'Not found',
        },
        status: 404,
      },
      message: 'Request failed',
      config: undefined,
    } as unknown as AxiosError<ErrorResponse>;

    const error = transformError(axiosError);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).path).toBe('');
  });

  it('should use default message for non-standard error without message', () => {
    const axiosError = {
      response: {
        data: null,
        status: 502,
      },
      message: '',
      config: { url: '/api/tasks' },
    } as unknown as AxiosError<ErrorResponse>;

    const error = transformError(axiosError);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).message).toBe('An unexpected error occurred');
  });
});
