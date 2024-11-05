import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './Header.css';

interface HeaderProps {
    onSearch?: (searchTerm: string, genderFilter: string) => void;
}

const Header: React.FC<HeaderProps> = ({ onSearch }) => {
    const [searchTerm, setSearchTerm] = useState('');
    const [genderFilter, setGenderFilter] = useState('all');
    const navigate = useNavigate();

    const handleSearch = () => {
        const trimmedSearchTerm = searchTerm.trim();
        if (trimmedSearchTerm.length < 2) {
            alert('검색어는 두 글자 이상이 필요합니다.');
            return;
        }

        if (onSearch) {
            onSearch(searchTerm, genderFilter);
        } else {
            navigate(`/search?query=${searchTerm}&gender=${genderFilter}`);
        }
    };

    const handleKeyPress = (event: React.KeyboardEvent<HTMLInputElement>) => {
        if (event.key === 'Enter') {
            handleSearch();
        }
    };

    const handleClear = () => {
        setSearchTerm('');
    };

    const handleGenderFilter = (filter: string) => {
        setGenderFilter(filter);
    };

    const handleGoHome = () => {
        navigate('/');
    };

    return (
        <header className="header">
            {/* 로고 이미지 */}
            <img
                src="/images/V-Song.png"
                alt="V-Song Logo"
                className="logo"
                onClick={handleGoHome}
                style={{ cursor: 'pointer', height: '40px' }} // 로고 클릭 시 홈으로 이동
            />

            <div className="search-bar">
                <input
                    type="text"
                    placeholder="검색"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    onKeyPress={handleKeyPress}
                />
                {searchTerm && (
                    <button type="button" className="clear-btn" onClick={handleClear}>
                        &#x2715;
                    </button>
                )}
                <button type="submit" className="search-btn" onClick={handleSearch}>
                    <img src="/images/SearchBar.png" alt="Search" />
                </button>
            </div>
        </header>
    );
};

export default Header;
