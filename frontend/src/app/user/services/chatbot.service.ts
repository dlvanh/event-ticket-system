import { Injectable } from '@angular/core';
import { Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap } from 'rxjs/operators';
import * as CryptoJS from 'crypto-js';

export interface ChatMessage {
  from: 'user' | 'bot';
  text: string;
}

@Injectable({ providedIn: 'root' })
export class ChatbotService {
  private apiUrl = 'http://localhost:8080/api';
  private storageKey = 'chatMessages';
  private messages: ChatMessage[] = [];
  private encryptionKey = 'eventa-secret-key';

  constructor(@Inject(PLATFORM_ID) private platformId: Object,
  private http: HttpClient) {
    this.loadMessages();
  }

  private encrypt(data: string): string {
    return CryptoJS.AES.encrypt(data, this.encryptionKey).toString();
  }

  private decrypt(encryptedData: string): string {
      const bytes = CryptoJS.AES.decrypt(encryptedData, this.encryptionKey);
      return bytes.toString(CryptoJS.enc.Utf8);
    }

    private loadMessages(): void {
    if (isPlatformBrowser(this.platformId)) {
      try {
        const saved = localStorage.getItem(this.storageKey);
        if (saved) {
          const decryptedData = this.decrypt(saved);
          this.messages = JSON.parse(decryptedData);
        }
      } catch (error) {
        console.error('Lỗi khi giải mã tin nhắn:', error);
        localStorage.removeItem(this.storageKey);
        this.messages = [];
      }
    }
  }

  getMessages(): ChatMessage[] {
    return this.messages;
  }

  addUserMessage(text: string): void {
    this.messages.push({ from: 'user', text });
    this.saveMessages();
  }

  addBotMessage(text: string): void {
    this.messages.push({ from: 'bot', text });
    this.saveMessages();
  }

  chat(message: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/chat`, { message }).pipe(
      tap((res: any) => {
        this.addBotMessage(res.reply);
      })
    );
  }

  saveMessages(): void {
    if (isPlatformBrowser(this.platformId)) {
      try {
        const dataToSave = JSON.stringify(this.messages);
        const encryptedData = this.encrypt(dataToSave);
        localStorage.setItem(this.storageKey, encryptedData);
      } catch (error) {
        console.error('Lỗi khi mã hóa tin nhắn:', error);
      }
    }
  }
  
  clearMessages(): void {
    this.messages = [];
    localStorage.removeItem(this.storageKey);
  }

  changeEncryptionKey(newKey: string): void {
    const currentMessages = [...this.messages];
    this.encryptionKey = newKey;
    this.messages = currentMessages;
    this.saveMessages();
  }
}