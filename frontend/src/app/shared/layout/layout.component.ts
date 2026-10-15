import {
  Component, OnInit, OnDestroy,
  Renderer2, HostListener, ViewChild, ElementRef
} from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { Subject, fromEvent } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';

interface NavItem      { route: string; icon: string; label: string; roles?: string[]; }
interface SearchResult { title: string; route: string; icon: string; }

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss']
})
export class LayoutComponent implements OnInit, OnDestroy {

  @ViewChild('searchInput') searchInput!: ElementRef<HTMLInputElement>;

  /* ── Navigation (toutes entrées avec filtrage par rôle) ─── */
  private readonly allNav: NavItem[] = [
    { route: '/dashboard',        icon: 'fas fa-gauge-high',      label: 'Tableau de bord' },
    { route: '/membres',          icon: 'fas fa-users',           label: 'Membres' },
    { route: '/mensualites',      icon: 'fas fa-money-bill-wave', label: 'Mensualités' },
    { route: '/tickets',          icon: 'fas fa-ticket',          label: 'Tickets' },
    { route: '/pointages',        icon: 'fas fa-qrcode',          label: 'Pointages' },
    { route: '/mes-transactions', icon: 'fas fa-list-check',      label: 'Mes transactions', roles: ['ROLE_GERANT'] },
    { route: '/sites',            icon: 'fas fa-building',        label: 'Salles',            roles: ['ROLE_ADMIN'] },
    { route: '/gerants',          icon: 'fas fa-user-tie',        label: 'Gérants',           roles: ['ROLE_ADMIN'] },
    { route: '/rapports',         icon: 'fas fa-chart-bar',       label: 'Rapports',          roles: ['ROLE_ADMIN'] },
    { route: '/parametres',       icon: 'fas fa-gear',            label: 'Paramètres',        roles: ['ROLE_ADMIN'] }
  ];

  /** Items filtrés selon le rôle courant. */
  get nav(): NavItem[] {
    const role = this.auth.getUserRole();
    return this.allNav.filter(item =>
      !item.roles || item.roles.includes(role ?? '')
    );
  }

  /* ── Utilisateur connecté ────────────────────────── */
  get userDisplayName(): string {
    const u = this.auth.currentUser;
    if (!u) return '';
    const nom = [u.prenom, u.nom].filter(Boolean).join(' ');
    return nom || u.username;
  }

  get userInitial(): string {
    return (this.userDisplayName.charAt(0) || '?').toUpperCase();
  }

  get userRoleLabel(): string {
    switch (this.auth.getUserRole()) {
      case 'ROLE_ADMIN':  return 'Super Admin';
      case 'ROLE_GERANT': return 'Gérant';
      default:            return '';
    }
  }

  /* ── Sidebar ─────────────────────────────────────── */
  sidebarMini = false;

  /* ── Dark mode ───────────────────────────────────── */
  isDarkMode = false;

  /* ── Fullscreen ──────────────────────────────────── */
  isFullscreen = false;

  /* ── Search ──────────────────────────────────────── */
  searchQuery       = '';
  isSearchFocused   = false;
  showSearchResults = false;
  searchResults: SearchResult[] = [];

  get quickActions(): NavItem[] {
    return this.nav.filter(n => n.route !== '/dashboard');
  }

  /* ── Profile ─────────────────────────────────────── */
  isProfileMenuOpen = false;

  /* ── Apparence modal ────────────────────────────────── */
  isThemeModalOpen = false;

  private readonly searchSubject = new Subject<string>();
  private readonly destroy$      = new Subject<void>();

  constructor(
    public  auth:     AuthService,
    private router:   Router,
    private renderer: Renderer2,
    private elRef:    ElementRef
  ) {
    this.setupSearchDebounce();
  }

  /* ════════════════ LIFECYCLE ════════════════════════ */

  ngOnInit(): void {
    // Le profil n'est mis en cache qu'au login : on le recharge après un F5
    if (!this.auth.currentUser) {
      this.auth.getCurrentUser().pipe(takeUntil(this.destroy$)).subscribe();
    }
    this.initSidebarState();
    this.initDarkMode();
    this.setupKeyboardShortcuts();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /* ════════════════ PAGE TITLE ══════════════════════ */

  getPageTitle(): string {
    const titles: Record<string, string> = {
      '/dashboard':        'Tableau de bord',
      '/membres':          'Gestion des membres',
      '/mensualites':      'Mensualités',
      '/tickets':          'Tickets',
      '/pointages':        'Pointages',
      '/mes-transactions': 'Mes transactions',
      '/sites':            'Salles & Sites',
      '/gerants':          'Gestion des gérants',
      '/rapports':         'Rapports',
      '/parametres':       'Paramètres'
    };
    const url = this.router.url.split('?')[0];
    const key = Object.keys(titles).find(k => url.startsWith(k)) ?? '';
    return titles[key] ?? 'METAPOWER GymTrack';
  }

  /* ════════════════ SIDEBAR ══════════════════════════ */

  private initSidebarState(): void {
    if (localStorage.getItem('sidebarMini') === 'true') {
      this.sidebarMini = true;
      this.applyContentMargin(false);
    }
  }

  toggleSidebar(): void {
    this.sidebarMini = !this.sidebarMini;
    localStorage.setItem('sidebarMini', String(this.sidebarMini));
    this.applyContentMargin(true);
  }

  private applyContentMargin(animate: boolean): void {
    requestAnimationFrame(() => {
      const el = this.elRef.nativeElement.querySelector('mat-sidenav-content');
      if (!el) return;
      el.style.transition = animate ? 'margin-left 0.3s cubic-bezier(0.4,0,0.2,1)' : 'none';
      el.style.marginLeft  = this.sidebarMini ? '68px' : '240px';
    });
  }

  /* ════════════════ DARK MODE ════════════════════════ */

  private initDarkMode(): void {
    if (localStorage.getItem('darkMode') === 'true') this.setDarkMode(true);
  }

  toggleTheme(): void { this.setDarkMode(!this.isDarkMode); }

  setDarkMode(enable: boolean): void {
    this.isDarkMode = enable;
    this.renderer.addClass(document.body, 'theme-transition');
    if (enable) {
      this.renderer.addClass(document.body, 'dark-mode');
    } else {
      this.renderer.removeClass(document.body, 'dark-mode');
    }
    localStorage.setItem('darkMode', String(enable));
    setTimeout(() => this.renderer.removeClass(document.body, 'theme-transition'), 400);
  }

  /* ════════════════ APPARENCE MODAL ══════════════════ */

  openThemeModal(): void {
    this.isThemeModalOpen  = true;
    this.isProfileMenuOpen = false;
  }

  closeThemeModal(): void { this.isThemeModalOpen = false; }

  /* ════════════════ FULLSCREEN ══════════════════════ */

  toggleFullscreen(): void {
    if (!this.isFullscreen) {
      document.documentElement.requestFullscreen?.();
    } else {
      document.exitFullscreen?.();
    }
  }

  @HostListener('document:fullscreenchange')
  onFullscreenChange(): void {
    this.isFullscreen = !!document.fullscreenElement;
  }

  /* ════════════════ SEARCH ══════════════════════════ */

  private setupSearchDebounce(): void {
    this.searchSubject.pipe(
      debounceTime(250),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(q => this.filterSearch(q));
  }

  private filterSearch(q: string): void {
    if (!q.trim()) { this.searchResults = []; return; }
    this.searchResults = this.nav
      .map(n => ({ title: n.label, route: n.route, icon: n.icon }))
      .filter(i => i.title.toLowerCase().includes(q.toLowerCase()));
  }

  /** Indique si l'utilisateur courant est un gérant. */
  get isGerant(): boolean { return this.auth.isGerant(); }
  get isAdmin(): boolean  { return this.auth.isAdmin(); }

  onSearchFocus(): void {
    this.isSearchFocused   = true;
    this.showSearchResults = true;
  }

  onSearchBlur(): void {
    setTimeout(() => {
      this.isSearchFocused   = false;
      this.showSearchResults = false;
    }, 250);
  }

  onSearchInput(): void { this.searchSubject.next(this.searchQuery); }

  performSearch(): void {
    if (this.searchResults.length > 0) this.navigateTo(this.searchResults[0].route);
  }

  clearSearch(): void {
    this.searchQuery       = '';
    this.searchResults     = [];
    this.showSearchResults = false;
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
    this.showSearchResults = false;
    this.isProfileMenuOpen = false;
  }

  /* ════════════════ PROFILE MENU ════════════════════ */

  toggleProfileMenu(event: Event): void {
    event.stopPropagation();
    this.isProfileMenuOpen = !this.isProfileMenuOpen;
  }

  /* ════════════════ GLOBAL CLICKS ═══════════════════ */

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event): void {
    const t = event.target as HTMLElement;
    if (!t.closest('.profile-dropdown')) this.isProfileMenuOpen  = false;
    if (!t.closest('.search-container')) this.showSearchResults  = false;
  }

  /* ════════════════ KEYBOARD ════════════════════════ */

  private setupKeyboardShortcuts(): void {
    fromEvent<KeyboardEvent>(document, 'keydown')
      .pipe(takeUntil(this.destroy$))
      .subscribe(e => {
        if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
          e.preventDefault();
          this.searchInput?.nativeElement?.focus();
          this.showSearchResults = true;
        }
        if ((e.ctrlKey || e.metaKey) && e.key === 'b') {
          e.preventDefault();
          this.toggleSidebar();
        }
        if (e.key === 'Escape') {
          this.isProfileMenuOpen = false;
          this.showSearchResults = false;
          this.closeThemeModal();
        }
      });
  }

  /* ════════════════ LOGOUT ══════════════════════════ */

  logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
